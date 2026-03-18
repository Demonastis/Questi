package com.example.sacdalancalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button add = findViewById(R.id.button);
        TextView text = findViewById(R.id.textView);
        EditText num1 = findViewById(R.id.editTextNumberDecimal);
        EditText num2 = findViewById(R.id.editTextNumberDecimal2);
        add.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num1Str = num1.getText().toString();
                String num2Str = num2.getText().toString();
                if(!num1Str.isEmpty()&&!num2Str.isEmpty()){
                    Double first = Double.parseDouble(num1Str);
                    Double second = Double.parseDouble(num2Str);
                    Double sum = first+second;
                    text.setText(String.valueOf(sum));
                } else{
                    Toast.makeText(MainActivity.this, "Please Add Number in Both", Toast.LENGTH_SHORT).show();
                }
            }
        }));
    }
}