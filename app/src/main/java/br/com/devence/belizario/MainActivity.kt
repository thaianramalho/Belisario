package br.com.devence.belizario

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
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
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel))
        })
        runOnUiThread(this)
    }

    private fun runOnUiThread(context: Context) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://thaianramalho.com/api_belizario/locaisAtendimento.php?senha=dxic5CyB")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog(context)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("Resposta da API", responseData ?: "API sem resposta")

                // Aqui você precisa analisar a resposta da API e adicionar marcadores ao mapa
                responseData?.let {
                    val localizacoesApi = Gson().fromJson(it, Array<LocalizacaoApi>::class.java)

                    runOnUiThread {
                        localizacoesApi.forEach { localizacao ->
                            val latLngArray = localizacao.latlng.split(",")
                            if (latLngArray.size == 2) {
                                val latitude = latLngArray[0].toDouble()
                                val longitude = latLngArray[1].toDouble()
                                val locInicial = LatLng(latitude, longitude)
                                googleMap.addMarker(
                                    MarkerOptions().position(locInicial).title(localizacao.nome)
                                )
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
