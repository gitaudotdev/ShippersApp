package com.codekid.shippersapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.codekid.shippersapp.Common.Common;
import com.codekid.shippersapp.Model.Shipper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import info.hoang8f.widget.FButton;

public class MainActivity extends AppCompatActivity {

    FButton btn_signIn;
    MaterialEditText edtPhone,edt_pass;

    FirebaseDatabase database;
    DatabaseReference shippers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn_signIn = findViewById(R.id.btn_signIn);

        edt_pass = findViewById(R.id.edtPassword);
        edtPhone = findViewById(R.id.edtPhone);

        database = FirebaseDatabase.getInstance();
        shippers = database.getReference(Common.SHIPPER_TABLE);
        
        btn_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login(edtPhone.getText().toString(),edt_pass.getText().toString());
            }
        });
    }

    private void login(String phone, final String password) {
        shippers.child(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            Shipper shipper = dataSnapshot.getValue(Shipper.class);
                            if (shipper.getPassword().equals(password))
                            {
                                //Login
                                startActivity(new Intent(MainActivity.this,HomeActivity.class));
                                Common.currentShipper =shipper;
                                finish();

                            }else
                            {
                                Toast.makeText(MainActivity.this, "Password is Incorrect", Toast.LENGTH_SHORT).show();
                            }
                        }else
                        {
                            Toast.makeText(MainActivity.this, "Your Credentials do not Exist!!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
