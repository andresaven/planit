package es.aam.planit;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;


public class Login extends AppCompatActivity {
    // Declaraciones de los botones y otros componentes de la UI
    Button buttonSingIn;
    Button buttonSingUp;
    Button buttonSingUpGoogle;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    EditText userText, passText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Configuración inicial de la Activity
        setTheme(R.style.Theme_Planit);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_login);

        // Animación del título de la aplicación
        AppCompatTextView tituloApp = findViewById(R.id.tituloApp);
        Animation slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        tituloApp.startAnimation(slideFromLeft);

        // Inicialización de Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Referencias a los EditText para usuario y contraseña
        userText = findViewById(R.id.emailBox);
        passText = findViewById(R.id.passBox);

        // Métodos para manejar los eventos de inicio de sesión, registro y Google Sign-In
        inicioContrasena();
        registro();
        google();

    }

    // Método para manejar el inicio de sesión con email y contraseña
    public void inicioContrasena() {
        buttonSingIn = findViewById(R.id.buttonSingIn);

        //CON LAMDA
        buttonSingIn.setOnClickListener(view -> {
            //Conectar con firebase para logearse
            String email = userText.getText().toString();
            String password = passText.getText().toString();

            //Validación
            if (email.isEmpty()) {
                userText.setError("Campo obligatorio");
            } else if (password.isEmpty()) {
                passText.setError("Campo obligatorio");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                userText.setError("Correo incorrecto");
            } else if (passText.length() < 6) {
                passText.setError("Mínimo 6 caracteres");
            } else{
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    //Pasar a la activity main
                                    Intent intent = new Intent(Login.this, MainActivity.class);
                                    startActivity(intent);

                                } else {
                                    Toast.makeText(Login.this, "Usuario o contraseña erróneos",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

        });
    }

    // Método para manejar el registro de nuevos usuarios
    public void registro() {
        buttonSingUp = findViewById(R.id.buttonSingUp);

        //SIN LAMDA
        buttonSingUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Conectar con firebase para registrarse
                String email = userText.getText().toString();
                String password = passText.getText().toString();

                //Validación
                if (email.isEmpty()) {
                    userText.setError("Campo obligatorio");
                } else if (password.isEmpty()) {
                    passText.setError("Campo obligatorio");
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    userText.setError("Correo incorrecto");
                } else if (passText.length() < 6) {
                    passText.setError("Mínimo 6 caracteres");
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            // Hay que borrar el this
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        // Sign in success, update UI with the signed-in user's information
/*                                    Log.d(TAG, "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);*/

                                        //Mensaje de entrada al crear usuario
                                        Toast.makeText(Login.this, "Usuario creado", Toast.LENGTH_SHORT).show();

                                        //Cambio de activity
                                        Intent intent = new Intent(Login.this, MainActivity.class);
                                        startActivity(intent);

                                    } else {
                                        // If sign in fails, display a message to the user.
                                        //Log.w(TAG, "createUserWithEmail:failure", task.getException());

                                        Toast.makeText(Login.this, "Registro erróneo",
                                                Toast.LENGTH_SHORT).show();

                                        //updateUI(null);
                                    }
                                }
                            });
                }

            }
        });
    }

    // Método para configurar y manejar el inicio de sesión con Google
    public void google() {
        // Configura las opciones de inicio de sesión de Google, incluyendo la solicitud del ID token y la dirección de correo.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Solicita un token ID para Firebase
                .requestEmail() // Solicita permiso para el correo electrónico
                .build(); // Crea las opciones de inicio de sesión de Google

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso); // Obtiene el cliente de inicio de sesión de Google

        // Registro para manejar el resultado del inicio de sesión de Google
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Manejo del resultado de Google Sign-In
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleSignInResult(task);
                    }
                }
        );

        // Configuración del botón de inicio de sesión con Google
        buttonSingUpGoogle = findViewById(R.id.buttonSingUpGoogle);
        buttonSingUpGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    // Método para iniciar el proceso de inicio de sesión con Google
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    // Método para manejar el resultado del inicio de sesión con Google
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken()); // Autenticar con Firebase usando el token de Google
        } catch (ApiException e) {
            // Manejar el error
        }
    }

    // Método para autenticar con Firebase utilizando el token de Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential) // Intentar autenticar al usuario en Firebase
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Acciones a realizar si la autenticación es exitosa
                        Intent intent = new Intent(Login.this, MainActivity.class);
                        startActivity(intent);
                    } else {
                        // Acciones a realizar si la autenticación falla
                        Toast.makeText(Login.this, "Error al iniciar con Google",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}