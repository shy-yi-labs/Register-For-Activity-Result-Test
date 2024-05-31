package com.example.registerforactivityresulttest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

/*
요구사항
- 권한 요구 설명 다이얼로그 띄우기
- 요청 조건 체크
- 결과 Pass/Fail 판정
- 사전 Permission 목록 정의
- launch 시점 result callback 적용
- 설정 이동 다이얼로그 띄우기
*/

class HomeFragment : Fragment() {
    private val launcher = registerReadExternalMediaPermissionRequest()
}

class MainActivity : AppCompatActivity() {

    private val launcher = registerReadExternalMediaPermissionRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.btClickMe)

        button.setOnClickListener {
            launcher.launch { _, isGranted ->
                if (isGranted) {
                    Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}