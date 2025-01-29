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
    private String serverAddress, clientId;
    private int port;

    // SharedPreferences for storing MQTT settings
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MQTTPreferences";
    private static final String KEY_SERVER_ADDRESS = "server_address";
    private static final String KEY_PORT = "port";
    private static final String KEY_DLC_ID = "dlc_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load saved preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        binding.editTextServerAddress.setText(sharedPreferences.getString(KEY_SERVER_ADDRESS, "0.tcp.in.ngrok.io")); // Default server address
        binding.editTextPort.setText(sharedPreferences.getString(KEY_PORT, ""));
        binding.editTextMacAddress.setText(sharedPreferences.getString(KEY_DLC_ID, ""));

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

        binding.buttonConnect.setOnClickListener(v -> {
            serverAddress = binding.editTextServerAddress.getText().toString();
            port = Integer.parseInt(binding.editTextPort.getText().toString());
            clientId = binding.editTextMacAddress.getText().toString();

            // Save settings in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SERVER_ADDRESS, serverAddress);
            editor.putString(KEY_PORT, String.valueOf(port));
            editor.putString(KEY_DLC_ID, clientId);
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

    private void connectToMqttBroker() {
        try {
            mqttClient = new MqttClient("tcp://" + serverAddress + ":" + port, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setKeepAliveInterval(60);
            options.setCleanSession(false);
            options.setAutomaticReconnect(true);

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
