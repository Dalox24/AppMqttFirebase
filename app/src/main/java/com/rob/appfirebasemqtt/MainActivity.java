package com.rob.appfirebasemqtt;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//librerias firebase y elementos App
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

//MQTT
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText txtId, txtNombre, txtPais;
    private ListView lista;
    private Spinner spContinente;
    private FirebaseFirestore db;
    String[] ListContinentes = {"Europa", "America del Norte", "America del Sur", "Asia", "África", "Oceanía"};
    private TextView textView;
    private EditText editTextMessage;
    private Button botonEnvio;
    private MqttClient mqttClient;

    private static String mqttHost = "tcp://volcanoparrot893.cloud.shiftr.io:1883"; //Ip servidor MQTT
    private static String IdUsuario = "AppAndroid"; //Nombre del Disp. que se conectará

    private static String Topico = "Mensaje"; //Topico que se Suscribirá
    private static String User = "volcanoparrot893"; //Usuario
    private static String Pass = "7G8BlKhGrMAjSamF"; //Token

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        CargarListaFirestore();
        //Inicializar Firestore
        db = FirebaseFirestore.getInstance();
        //
        txtId = findViewById(R.id.txtId);
        txtNombre = findViewById(R.id.txtNombre);
        txtPais = findViewById(R.id.txtPais);
        spContinente = findViewById(R.id.spContinente);
        lista = findViewById(R.id.lista);
        botonEnvio = findViewById(R.id.btnEnviarDatos);
        //
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ListContinentes);
        spContinente.setAdapter(adapter);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            mqttClient.connect(options);
            Toast.makeText(this, "Aplicación Conectada a Servidor MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT","Conexión Perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completada");
                }
            });
        }
        catch (MqttException e){
            e.printStackTrace();
        }
        //-----------------------
        botonEnvio.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //obtener el mensaje ingresado por el usuario
                String mensaje = txtNombre.getText().toString()+ "|" + txtPais.getText().toString();
                try {
                    //verifico si la conexion MQTT esta activa
                    if (mqttClient != null && mqttClient.isConnected()){
                        //publicar el mensaje en el topico especificado
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        //mostrar el mensaje enviado en el textView
                        textView.append("\n - "+ mensaje);
                        Toast.makeText(MainActivity.this,"Mensaje Enviado",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Error: No se pudo enviar el mensaje. La conexión MQTT no está activa.", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (MqttException e){
                    e.printStackTrace();
                }
            }
        });
        //--------------------
    }
    public void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("equipos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            List<String> listaEquipos = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()){
                                String linea = "|"+ document.getString("idEquipo") + "|"+
                                        document.getString("nombre") + "|"  +
                                        document.getString("Continente") + "|"  +
                                        document.getString("pais") + "|";
                                listaEquipos.add(linea);
                            }
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_list_item_1,listaEquipos);
                            lista.setAdapter(adaptador);
                        }
                        else {
                            Log.e("TAG", "Error al obtener datos de firestore", task.getException());
                        }
                    }


                });



    }
    public void enviarDatosFirestore(View view){
        String idEquipo = txtId.getText().toString();
        String nombre = txtNombre.getText().toString();
        String pais = txtPais.getText().toString();
        String continente = spContinente.getSelectedItem().toString();
        //
        Map<String, Object> equipos = new HashMap<>();
        equipos.put("idEquipos", idEquipo);
        equipos.put("nombre", nombre);
        equipos.put("pais", pais);
        equipos.put("Continente", continente);

        //
        db.collection("equipos")
                .document(idEquipo)
                .set(equipos)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this,"Datos Enviados a Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this,"Error al enviar los datos a Firestore: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                });


    }
    public void CargarLista(View view){
        CargarListaFirestore();
    }

}