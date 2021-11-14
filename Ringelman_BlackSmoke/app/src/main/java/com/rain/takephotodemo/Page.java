package com.rain.takephotodemo;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class Page extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        Button button_hello = (Button) this.findViewById(R.id.hello);
        Button button_devpage = (Button) this.findViewById(R.id.btn_name);

        button_hello.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Page.this,MainActivity.class);
                startActivity(intent);
            }
        });

        button_devpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Page.this,Dev_Page.class);
                startActivity(intent);
            }
        });
    }
}