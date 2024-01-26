package es.aam.planit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private FloatingActionButton fab;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String idUser;
    ListView listView;

    //Para rellenar el listView
    ArrayAdapter<String> taskAdapter;

    //Auxiliares
    List<String> taskList = new ArrayList<>();
    List<String> taskListId = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topAppBar = findViewById(R.id.topAppBar);

        mAuth = FirebaseAuth.getInstance();
        idUser = mAuth.getCurrentUser().getUid();

        fab = findViewById(R.id.fab);
        addTask();

        listView = findViewById(R.id.listTask);
        db = FirebaseFirestore.getInstance();
        updateUI();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Deseas cerrar tu sesión?")
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAuth.signOut();
                            startActivity(new Intent(MainActivity.this, Login.class));
                            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void addTask() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                final EditText taskEditText = new EditText(MainActivity.this);
                taskEditText.requestFocus();
                AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Nueva tarea")
                        .setMessage("Planifica una nueva tarea")
                        .setView(taskEditText)
                        .setPositiveButton("Añadir", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Añadir tarea a la base de datos
                                String task = taskEditText.getText().toString();

                                Map<String, Object> data = new HashMap<>();
                                data.put("taskName", task);
                                data.put("user", idUser);

                                db.collection("Tasks")
                                        .add(data)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Toast.makeText(MainActivity.this, "Tarea creada", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(MainActivity.this, "Error al crear la tarea", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            }
                        })

                        .setNegativeButton("Cancelar", null)
                        .create();
                dialog.show();
            }
        });
    }

    public void updateUI() {
        db.collection("Tasks")
                .whereEqualTo("user", idUser)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        //Reseteamos los arrays para que vengan vacíos
                        taskList.clear();
                        taskListId.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            //Añadimos los datos a los arrays
                            taskList.add(doc.getString("taskName"));
                            taskListId.add(doc.getId());
                        }

                        //Rellenar el ListView con el adapter
                        if (taskList.size() == 0) {
                            listView.setAdapter(null);
                        } else {
                            taskAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.item_task, R.id.textViewTask, taskList);
                            listView.setAdapter(taskAdapter);
                        }
                    }
                });
    }

    public void doneTask (View view) {
        View parent = (View) listView.getParent();
        AppCompatTextView textViewTask = parent.findViewById(R.id.textViewTask);
        String task = textViewTask.getText().toString();
        int position = taskList.indexOf(task);

        db.collection("Tasks").document(taskListId.get(position)).delete();
        }

    public void updateTask (View view) {
        AppCompatTextView textViewTask = findViewById(R.id.textViewTask);
        String task = textViewTask.getText().toString();
        final EditText taskEditText = new EditText(MainActivity.this);
        int position = taskList.indexOf(task);
        taskEditText.setText(task);

            AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Editar tarea")
                    .setMessage("Realiza los cambios necesarios")
                    .setView(taskEditText)
                    .setPositiveButton("Modificar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Modificar tarea a la base de datos
                            String updateTask = taskEditText.getText().toString();
                            Map<String, Object> data = new HashMap<>();
                            data.put("taskName", updateTask);
                            data.put("user", idUser);

                            db.collection("Tasks")
                                    .document(taskListId.get(position))
                                    .update(data)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(MainActivity.this, "Tarea actualizada", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Error al modificar la tarea", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }
                    })

                    .setNegativeButton("Cancelar", null)
                    .create();
            dialog.show();
        }



}



