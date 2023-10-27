package br.com.devence.belizario

import android.content.Context
import okhttp3.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

data class Localizacao(val nome: String, val latitude: Double, val longitude: Double)

val locubs = listOf(
    Localizacao(
        "Unidade Básica de Saúde (UBS) - João Paulo II", -21.19596944477334, -43.792077649345096
    ),
    Localizacao("Posto de Saúde 9 de Março", -21.19937326912291, -43.79791970037965),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Funcionários", -21.216469326057638, -43.78344099378308
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Boa Vista", -21.218134981999928, -43.762208247500844
    ),
    Localizacao("Unidade Básica de Saúde (UBS) - Vilela", -21.219737088847214, -43.753605708677746),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Guido Roman", -21.257561598991014, -43.807042117087065
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Santa Efigênia", -21.206719978184385, -43.764625920257984
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Santa Cecília", -21.223178665810803, -43.79229997681931
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Grogotó", -21.203372975440658, -43.776995547244795
    ),
    Localizacao("Unidade Básica de Saúde (UBS) - Carmo", -21.242409531959435, -43.7686812820244),
    Localizacao(
        "Unidade Básica de Saúde (UBS) Nova Suíça", -21.224963395389924, -43.74178558474852
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - Correia de Almeida",
        -21.302449026698287,
        -43.620601359415325
    ),
    Localizacao(
        "Unidade Básica de Saúde (UBS) - São Francisco", -21.201007843020285, -43.76970781893588
    ),
    Localizacao(
        "Hospital Regional de Barbacena Dr. José Américo", -21.204068595595945, -43.787800823882115
    ),
    Localizacao("Hospital Ibiapaba CEBAMS", -21.220730344412804, -43.77773563058238),
    Localizacao(
        "Centro de Atenção Psicossocial - CAPS AD III", -21.198146533376402, -43.7800255643328
    ),
    Localizacao("CAPS TM III", -21.198146533376402, -43.7800255643328),
    Localizacao(
        "Centro de Atenção Psicossocial Infanto-Juvenil (CAPSi)",
        -21.225087570493017,
        -43.77007137903121
    )
)

class MainActivity : AppCompatActivity() {

    private var mContext: Context? = null
    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it

            val locInicial = LatLng(-21.19596944477334, -43.792077649345096)
            val zoomLevel = 12f
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel));


            locubs.forEach { localizacao ->
                val locInicial = LatLng(localizacao.latitude, localizacao.longitude)
                googleMap.addMarker(MarkerOptions().position(locInicial).title(localizacao.nome))
            }
        })
        runOnUiThread(this)
    }

    private fun runOnUiThread(context: Context) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://thaianramalho.com/colinas/api_request.php?senha=dxic5CyB&uid=").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog(context)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                // Manipule a resposta da API aqui
            }
        })
    }

    private fun showErrorDialog(context: Context) {
        runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Erro")
            builder.setMessage("Ocorreu um erro, entre em contato com o proprietário do aplicativo.")
            builder.setPositiveButton("OK", null)
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
}