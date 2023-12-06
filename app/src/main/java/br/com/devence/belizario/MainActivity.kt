package br.com.devence.belizario

import LocationAdapter
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.text.Normalizer

data class LocalizacaoApi(val nome: String, val latlng: String)

class MainActivity : AppCompatActivity() {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private val markerList = mutableListOf<Marker>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.inputBusca)
        val inputBusca = findViewById<EditText>(R.id.inputBusca)
        val confirmBusca = findViewById<Button>(R.id.confirmBusca)
        val locInicial = LatLng(-21.19596944477334, -43.792077649345096)
        val zoomLevel = 12f

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://thaianramalho.com/api_belizario/sintomas.php?senha=dxic5CyB").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog(this@MainActivity)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                responseData?.let {
                    val jsonArray = JSONArray(it)
                    val suggestions = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nome = obj.getString("nome")
                        suggestions.add(nome)
                    }
                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            suggestions
                        )
                        autoCompleteTextView.setAdapter(adapter)
                        autoCompleteTextView.threshold = 1
                        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
                            val selectedItem = parent.getItemAtPosition(position) as String
                            autoCompleteTextView.setText(selectedItem, false)
                        }
                        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                adapter.filter.filter(s, object : Filter.FilterListener {
                                    override fun onFilterComplete(count: Int) {
                                        if (count == 0) {
                                            autoCompleteTextView.dismissDropDown()
                                        } else {
                                            autoCompleteTextView.showDropDown()
                                        }
                                    }
                                })
                            }
                        })
                    }
                }
            }
        })

        confirmBusca.setOnClickListener {
            val textoBusca = inputBusca.text.toString()
            runOnUiThread(this@MainActivity, textoBusca)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it

            locInicial
            zoomLevel
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel))
        })

    }

    private fun removeAccents(input: String): String {
        val regexUnaccent = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        val temp = Normalizer.normalize(input, Normalizer.Form.NFD)
        return regexUnaccent.replace(temp, "")
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
            .url("https://thaianramalho.com/api_belizario/atendimento.php?senha=dxic5CyB&sintoma=${textoBusca}")
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
                        val jsonArray = JSONArray(it)
                        val localizacoesApi =
                            Gson().fromJson(it, Array<LocalizacaoApi>::class.java).toList()


                        runOnUiThread {
                            for (marker in markerList) {
                                marker.remove()
                            }
                            markerList.clear()

                            val listaLocais = findViewById<TextView>(R.id.listaLocais)
                            listaLocais.visibility = View.VISIBLE

                            localizacoesApi.forEach { localizacao ->
                                val btn = Button(this@MainActivity)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, 16, 0, 0)
                                btn.layoutParams = params

                                val buttonText = "<b>${localizacao.nome}</b><br/>Lat: ${
                                    localizacao.latlng.split(",")[0]
                                }, Lng: ${localizacao.latlng.split(",")[1]}"
                                btn.text = HtmlCompat.fromHtml(
                                    buttonText,
                                    HtmlCompat.FROM_HTML_MODE_COMPACT
                                )
                                btn.setBackgroundResource(android.R.drawable.btn_default)
                                btn.setOnClickListener {
                                    val latLngArray = localizacao.latlng.split(",")
                                    if (latLngArray.size == 2) {
                                        val latitude = latLngArray[0].toDouble()
                                        val longitude = latLngArray[1].toDouble()
                                        val loc = LatLng(latitude, longitude)
                                        val zoomLevel = 12f
                                        googleMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                loc,
                                                zoomLevel
                                            )
                                        )
                                    }
                                }

                                val layout = findViewById<LinearLayout>(R.id.layoutPrincipal)
                                layout.addView(btn)
                            }


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
                            if (markerList.isNotEmpty()) {
                                val bounds = LatLngBounds.builder()
                                for (marker in markerList) {
                                    bounds.include(marker.position)
                                }
                                googleMap.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        bounds.build(),
                                        100
                                    )
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
            builder.setMessage("Ocorreu um erro na busca. Se o problema persistir, verifique sua conexão com a internet ou entre em contato com o proprietário do aplicativo.")
            builder.setPositiveButton("OK", null)
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

}
