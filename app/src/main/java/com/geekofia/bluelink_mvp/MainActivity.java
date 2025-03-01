package com.geekofia.bluelink_mvp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.geekofia.bluelink_mvp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MqttClient mqttClient;
    private String serverAddress, clientId, protocol, username, password;
    private boolean isAuthEnabled;

    // SharedPreferences for storing MQTT settings
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MQTTPreferences";
    private static final String KEY_SERVER_ADDRESS = "server_address";
    private static final String KEY_DLC_ID = "dlc_id";
    private static final String KEY_PROTOCOL = "protocol";
    private static final String KEY_AUTH_ENABLED = "auth_enabled";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load saved preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
//        binding.editTextServerAddress.setText(sharedPreferences.getString(KEY_SERVER_ADDRESS, "mqtt.dugulink.xyz"));
        binding.editTextMacAddress.setText(sharedPreferences.getString(KEY_DLC_ID, "DLCA842E35A837C"));

        // Load protocol
        protocol = sharedPreferences.getString(KEY_PROTOCOL, "ssl://");
        // Protocol Selection
        binding.radioGroupProtocol.setOnCheckedChangeListener((group, checkedId) -> {
            // Check which radio button is selected and set the protocol accordingly
            if (checkedId == R.id.radioButtonSsl) {
                protocol = "ssl://"; // Use SSL protocol
            } else if (checkedId == R.id.radioButtonNoSsl) {
                protocol = "tcp://"; // Use Non-SSL protocol
            }
        });

        // Load authentication settings
        isAuthEnabled = sharedPreferences.getBoolean(KEY_AUTH_ENABLED, true);
//        binding.checkBoxAuth.setChecked(isAuthEnabled);
//        binding.editTextUsername.setText(sharedPreferences.getString(KEY_USERNAME, ""));
//        binding.editTextPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));
//        toggleAuthFields(isAuthEnabled);


        // Auto-uppercase MAC address
        binding.editTextMacAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if (!input.equals(input.toUpperCase())) {
                    binding.editTextMacAddress.removeTextChangedListener(this);
                    binding.editTextMacAddress.setText(input.toUpperCase());
                    binding.editTextMacAddress.setSelection(input.length());
                    binding.editTextMacAddress.addTextChangedListener(this);
                }
            }
        });

        // Toggle Auth Fields
//        binding.checkBoxAuth.setOnCheckedChangeListener((buttonView, isChecked) -> toggleAuthFields(isChecked));

        binding.buttonConnect.setOnClickListener(v -> {
//            serverAddress = binding.editTextServerAddress.getText().toString();
            serverAddress = "mqtt.dugulink.xyz";
            clientId = binding.editTextMacAddress.getText().toString();
//            isAuthEnabled = binding.checkBoxAuth.isChecked();
//            username = binding.editTextUsername.getText().toString();
//            password = binding.editTextPassword.getText().toString();
            username = "ubuntu";
            password = "chandu";

            // Save settings in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SERVER_ADDRESS, serverAddress);
            editor.putString(KEY_DLC_ID, clientId);
            editor.putString(KEY_PROTOCOL, protocol);
            editor.putBoolean(KEY_AUTH_ENABLED, isAuthEnabled);
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PASSWORD, password);
            editor.apply();

            connectToMqttBroker();
        });

        binding.buttonAdd.setOnClickListener(v -> {
            String pinNo = binding.editTextPin.getText().toString();
            if (!pinNo.isEmpty()) {
                publishMessage("ADD:" + pinNo);
            }
        });

        binding.buttonOn.setOnClickListener(v -> {
            String pinNo = binding.editTextPin.getText().toString();
            if (!pinNo.isEmpty()) {
                publishMessage("SET:" + pinNo + ":ON");
            }
        });

        binding.buttonOff.setOnClickListener(v -> {
            String pinNo = binding.editTextPin.getText().toString();
            if (!pinNo.isEmpty()) {
                publishMessage("SET:" + pinNo + ":OFF");
            }
        });
    }

//    private void toggleAuthFields(boolean show) {
//        binding.layoutAuthFields.setVisibility(show ? View.VISIBLE : View.GONE);
//    }

    private void connectToMqttBroker() {
        try {
            // Ensure the protocol is set, default to ssl:// if empty
            if (protocol == null || protocol.isEmpty()) {
                protocol = "ssl://"; // Set default to SSL if protocol is not set
            }

            String brokerUrl = protocol + serverAddress + ":" + (protocol.equals("ssl://") ? 8883 : 1883);
            mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60);
            options.setCleanSession(false);
            options.setAutomaticReconnect(true);

            if (isAuthEnabled) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT_CLIENT", "Connection Lost: " + cause.getMessage());
                    runOnUiThread(() -> {
                        binding.layoutCommands.setVisibility(View.GONE); // Show command layout
                        Toast.makeText(getApplicationContext(), "Connection Lost to MQTT broker", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String receivedMessage = new String(message.getPayload());
                    Log.d("MQTT_CLIENT", "Message received: " + receivedMessage);
                    // Display received message
                    runOnUiThread(() -> {
                        String existingText = binding.textViewReceivedMessage.getText().toString();
                        binding.textViewReceivedMessage.setText(existingText + "\n" + receivedMessage); // Append message with a newline for readability
                        binding.textViewReceivedMessage.setVisibility(View.VISIBLE); // Ensure it's visible
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT_CLIENT", "Message delivery complete");
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe("dugulink/client/" + clientId + "/acknowledgments", 1);

            runOnUiThread(() -> {
                binding.layoutCommands.setVisibility(View.VISIBLE); // Show command layout
                Toast.makeText(getApplicationContext(), "Connected to MQTT broker", Toast.LENGTH_SHORT).show();
            });

        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to connect to MQTT broker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void publishMessage(String command) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(command.getBytes());
                message.setQos(1);
                mqttClient.publish("dugulink/client/" + clientId + "/commands", message);
                Toast.makeText(this, "Command published successfully", Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to publish message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "MQTT client is not connected", Toast.LENGTH_SHORT).show();
        }
    }
}
