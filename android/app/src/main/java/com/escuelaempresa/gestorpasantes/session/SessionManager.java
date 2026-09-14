package com.escuelaempresa.gestorpasantes.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

// Guarda el JWT cifrado en disco (EncryptedSharedPreferences), no en preferencias
// planas: es el token que autentica a la app entera, asi que vale la pena.
public final class SessionManager {

    private static final String ARCHIVO = "sesion_segura";
    private static final String CLAVE_TOKEN = "jwt";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    ARCHIVO,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("No se pudo inicializar el almacenamiento seguro", e);
        }
    }

    public void guardarToken(String token) {
        prefs.edit().putString(CLAVE_TOKEN, token).apply();
    }

    public String obtenerToken() {
        return prefs.getString(CLAVE_TOKEN, null);
    }

    public boolean haySesion() {
        return obtenerToken() != null;
    }

    public void cerrarSesion() {
        prefs.edit().clear().apply();
    }
}
