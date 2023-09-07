package com.abdullah.duvarkagitlariadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> kategoriler;
    private ArrayAdapter<String> adapter;
    private Toolbar toolbar;
    private Spinner spinner;
    private Button buttonSec, buttonYukle;
    private ImageView imageView;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 22;

    private FirebaseStorage storage;
    private StorageReference storageReference;
    private DatabaseReference reference;

    private String kategori;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!isConnected()) {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

            alertDialog.setIcon(R.drawable.internet);
            alertDialog.setTitle("İnternet Erişimi Uyarısı");
            alertDialog.setMessage("Lütfen internet bağlantınızı kontrol ediniz.");

            alertDialog.setPositiveButton("Kapat", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    finish();
                }
            });

            AlertDialog alert = alertDialog.create();

            alert.setCanceledOnTouchOutside(false);
            alert.show();
        }

        init();
    }

    private void init() {

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        kategoriler = new ArrayList<>();

        kategoriler.add("Hayvanlar");
        kategoriler.add("Renkler");
        kategoriler.add("Siyah");
        kategoriler.add("Diğer");

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, kategoriler);

        buttonSec = findViewById(R.id.buttonSec);
        buttonYukle = findViewById(R.id.buttonYukle);
        spinner = findViewById(R.id.spinner);
        imageView = findViewById(R.id.imageView);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        buttonSec.setOnClickListener(v -> selectImage());

        buttonYukle.setOnClickListener(v -> uploadImage());

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                kategori = kategoriler.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {


            }
        });
    }

    private boolean isConnected() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    private void selectImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Resim seçiniz..."), PICK_IMAGE_REQUEST);
    }

    private void uploadImage() {

        if (filePath != null) {

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Yükleniyor...");
            progressDialog.show();

            final StorageReference ref = storageReference.child(kategori + "/" + UUID.randomUUID().toString());

            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                        @Override
                        public void onSuccess(Uri uri) {

                            Uri downloadUri = uri;

                            reference = FirebaseDatabase.getInstance().getReference(kategori + "/"
                                    + UUID.randomUUID()).child("url");

                            reference.setValue(downloadUri.toString());
                        }
                    });

                    progressDialog.dismiss();

                    Toast.makeText(MainActivity.this, "Resim yüklendi.",
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {

                @Override
                public void onFailure(@NonNull Exception e)
                {
                    progressDialog.dismiss();

                    Toast.makeText(MainActivity.this, "Başarısız!" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {

                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Yükleniyor " + (int)progress + "%");
                }
            });
        }

        else {

            Toast.makeText(MainActivity.this, "Bir resim seçiniz.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null
                && data.getData() != null) {

            filePath = data.getData();

            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            }

            catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}