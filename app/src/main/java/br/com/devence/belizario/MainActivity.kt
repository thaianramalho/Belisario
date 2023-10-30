package br.com.devence.belizario

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.widget.Button
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputBusca = findViewById<EditText>(R.id.inputBusca)
        val confirmBusca = findViewById<Button>(R.id.confirmBusca)

        confirmBusca.setOnClickListener {
            val textoBusca = inputBusca.text.toString()
            runOnUiThread(this@MainActivity, textoBusca)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it

            val locInicial = LatLng(-21.19596944477334, -43.792077649345096)
            val zoomLevel = 12f
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel))
        })

    }

    private fun msgNenhumResultadoEncontrado() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Nenhum local encontrado.").setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val alert = dialogBuilder.create()
        alert.setTitle("Aviso")
        alert.show()
    }

    private fun limparInput(input: EditText) {
        input.text.clear()
        markerList.clear()

    }

    private fun runOnUiThread(context: Context, textoBusca: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
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
                        val inputBusca = findViewById<EditText>(R.id.inputBusca)

                        limparInput(inputBusca)
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
