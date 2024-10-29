package com.conect.aplicativoconect.view.ui.admin

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R

class AdminProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile) // Certifique-se de carregar o layout da atividade que tem o FrameLayout
    }
}
