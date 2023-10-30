package br.com.devence.belizario

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

data class LocalizacaoApi(val nome: String, val latlng: String)

class MainActivity : AppCompatActivity() {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private val markerList = mutableListOf<Marker>()
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it

            val locInicial = LatLng(-21.19596944477334, -43.792077649345096)
            val zoomLevel = 12f
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel))
        })
        val inputBusca = findViewById<EditText>(R.id.inputBusca)
        inputBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                var textoBusca = s.toString()

                runOnUiThread(this@MainActivity, textoBusca)
            }
        })

        inputBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                var textoBusca = s.toString()
                runnable?.let { handler.removeCallbacks(it) }
                runnable = Runnable { runOnUiThread(this@MainActivity, textoBusca) }
                handler.postDelayed(runnable!!, 500)
            }
        })
    }
    // Dentro da classe MainActivity, adicione a função abaixo
    private fun msgNenhumResultadoEncontrado() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Nenhum local encontrado.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val alert = dialogBuilder.create()
        alert.setTitle("Aviso")
        alert.show()
    }

    private fun runOnUiThread(context: Context, textoBusca: String) {
        val client = OkHttpClient()
        var request = Request.Builder()
            .url("https://thaianramalho.com/api_belizario/locaisAtendimento.php?senha=dxic5CyB&busca=$textoBusca")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog(context)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                if (responseData == "Nenhum resultado encontrado.") {
                    runOnUiThread {
                        msgNenhumResultadoEncontrado()
                    }
                } else {
                    responseData?.let {
                        val localizacoesApi = Gson().fromJson(it, Array<LocalizacaoApi>::class.java)

                        runOnUiThread {
                            for (marker in markerList) {
                                marker.remove()
                            }
                            markerList.clear()

                            localizacoesApi.forEach { localizacao ->
                                val latLngArray = localizacao.latlng.split(",")
                                if (latLngArray.size == 2) {
                                    val latitude = latLngArray[0].toDouble()
                                    val longitude = latLngArray[1].toDouble()
                                    val locInicial = LatLng(latitude, longitude)
                                    val marker = googleMap.addMarker(
                                        MarkerOptions().position(locInicial).title(localizacao.nome)
                                    )
                                    if (marker != null) {
                                        markerList.add(marker)
                                    } else {
                                        markerList.clear()
                                    }
                                }
                            }
                        }
                    }
                }
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
