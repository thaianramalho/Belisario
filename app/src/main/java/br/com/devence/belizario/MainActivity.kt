package br.com.devence.belizario

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Filter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

data class LocalizacaoApi(val nome: String, val latlng: String)

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private val markerList = mutableListOf<Marker>()
    private var markerMaisProximo: LatLng? = null
    private var distanciaMaisProxima: Float = Float.MAX_VALUE


    val filter = object : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            return source?.toString()?.replace("\n", "")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tamanhoLayoutBusca(-400)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener(this)


        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacaoAtual()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.inputBusca)
        val inputBusca = findViewById<EditText>(R.id.inputBusca)
        val confirmBusca = findViewById<ImageButton>(R.id.confirmBusca)
        val limparInputBusca = findViewById<ImageButton>(R.id.limparInput)
        val locInicial = LatLng(-21.22332575411119, -43.77215283547053)
        val zoomLevel = 13f

        val inputsLayout = findViewById<ConstraintLayout>(R.id.main)

        inputsLayout.setOnTouchListener { _, _ ->
            hideKeyboard(autoCompleteTextView)
            false
        }


        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://thaianramalho.com/api_belizario/sintomas.php?senha=dxic5CyB").build()

        limparInputBusca.setOnClickListener {
            limparInput(inputBusca)
        }

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
                        autoCompleteTextView.filters = arrayOf(filter)
                        autoCompleteTextView.threshold = 1
                        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
                            val selectedItem = parent.getItemAtPosition(position) as String
                            autoCompleteTextView.setText(selectedItem, false)
                        }

                        autoCompleteTextView.setOnItemClickListener { _, _, _, _ ->
                            hideKeyboard(autoCompleteTextView)
                        }
                        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {}
                            override fun beforeTextChanged(
                                s: CharSequence?, start: Int, count: Int, after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence?, start: Int, before: Int, count: Int
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

            if (textoBusca.isNotEmpty()) {
                tamanhoLayoutBusca(0)
                markerMaisProximo = null
                distanciaMaisProxima = Float.MAX_VALUE

                for (marker in markerList) {
                    marker.remove()
                }
                markerList.clear()

                runOnUiThread(this@MainActivity, textoBusca, locInicial)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Digite algum sintoma para realizar a busca.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(OnMapReadyCallback {
            googleMap = it

            locInicial
            zoomLevel
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locInicial, zoomLevel))
        })

        val faqButton = findViewById<FloatingActionButton>(R.id.sidebarButton)

        faqButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.perguntas_frequentes -> {
                exibirListaPerguntasFrequentes()
            }
            R.id.lista_ubs_bairros -> {
                exibirListaUbsBairros()
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    private fun exibirListaUbsBairros(){
        val listaUbsBairros = listOf(
            FaqItem(
                "A cidade possui diversas UBS, sendo que cada uma delas atende a bairros específicos." +
                        "\n\nA UBS que lhe atenderá é a que está listada com o nome do bairro onde você mora.\n\n" +
                        "Confira abaixo a lista e descubra qual UBS atende seu bairro!",
                ""
            ),
            FaqItem(
                "UBS Vilela, UBS Santa Efigênia, UBS São Pedro, UBS Santo Antônio",
                "Água Santa \n" +
                        "Caeté\n" +
                        "Caminho Novo\n" +
                        "Guarani\n" +
                        "Novo Horizonte\n" +
                        "Santa Efigênia\n" +
                        "Santo Antônio\n" +
                        "São Pedro"
            ),
            FaqItem(
                "UBS Vilela e UBS Boa Vista",
                "Andaraí\n" +
                        "Boa Vista \n" +
                        "Caiçaras\n" +
                        "Chácara das Andorinhas\n" +
                        "Passarinhos\n" +
                        "Penha\n" +
                        "Pontilhão\n" +
                        "São José\n" +
                        "Tijuca \n" +
                        "Vilela"
            ),
            FaqItem(
                "UBS Nova Suíça",
                "Ipanema\n" +
                        "Jardim das Alterosas\n" +
                        "Nova Cidade\n" +
                        "Nova Suíça"
            ),
            FaqItem(
                "UBS Carmo",
                "Boa Morte\n" +
                        "Bom Pastor\n" +
                        "Campo \n" +
                        "Carmo\n" +
                        "Dom Bosco\n" +
                        "Fátima\n" +
                        "Jardim\n" +
                        "Loteamento Ceolin\n" +
                        "Ponte do Cosme\n" +
                        "São Geraldo\n" +
                        "Sapé\n" +
                        "Serra Verde\n" +
                        "UBS Funcionários\n" +
                        "Centro\n" +
                        "Diniz I\n" +
                        "Diniz II\n" +
                        "Floresta\n" +
                        "Funcionários\n" +
                        "Mansões \n" +
                        "Nossa Senhora Aparecida\n" +
                        "Padre Cunha\n" +
                        "São Sebastião"
            ),
            FaqItem(
                "UBS Grogotó, UBS João Paulo II, UBS São Francisco, UBS Nove de Março",
                "Bananal\n" +
                        "Grogotó \n" +
                        "Jacó\n" +
                        "João Paulo II\n" +
                        "Loteamento Loschi\n" +
                        "Nove de Março\n" +
                        "Panorama\n" +
                        "Rosa Park\n" +
                        "Santa Luzia\n" +
                        "Santa Maria\n" +
                        "São Francisco\n" +
                        "Serrão\n" +
                        "Vale das Rosas\n" +
                        "Vista Alegre"
            ),
            FaqItem(
                "UBS Santa Cecília",
                "Eucisa\n" +
                        "Faria\n" +
                        "Monsenhor Mário Quintão\n" +
                        "Monte Mário\n" +
                        "Residencial Savassi\n" +
                        "Retiro das Rosas\n" +
                        "Santa Cecília\n" +
                        "São Cristóvão\n" +
                        "São Jorge\n" +
                        "São Vicente de Paulo\n" +
                        "Valentin Prenassi"
            ),
            FaqItem(
                "UBS Correia de Almeida, UBS Torres",
                "Campestre II\n" +
                        "Correia de Almeida\n" +
                        "Costas\n" +
                        "Galego\n" +
                        "Mantiqueira\n" +
                        "Palmital\n" +
                        "Quinta das Mantiqueiras\n" +
                        "Torres"
            ),
            FaqItem(
                "UBS Senhora das Dores, UBS Pinheiro Grosso",
                "Margaridas\n" +
                        "Pinheiro Grosso\n" +
                        "Senhora das Dores"
            ),
            FaqItem(
                "UBS Padre Brito",
                "Padre Brito"
            ),
            FaqItem(
                "UBS Guido Roman",
                "Belvedere\n" +
                        "Guido Roman\n" +
                        "Santa Tereza I\n" +
                        "Santa Tereza II"
            ),
        )
        val scrollView = ScrollView(this)
        scrollView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(20, 20, 20, 20)

        val marginInDp = 15

        for ((index, listaUbsBairro) in listaUbsBairros.withIndex()) {
            val perguntaTextView = TextView(this)
            perguntaTextView.text = listaUbsBairro.pergunta
            perguntaTextView.textSize = 18f
            linearLayout.addView(perguntaTextView)

            val respostaTextView = TextView(this)
            respostaTextView.text = listaUbsBairro.resposta
            respostaTextView.textSize = 14f
            respostaTextView.setTextColor(resources.getColor(android.R.color.darker_gray))
            respostaTextView.setPadding(0, 0, 0, 20)
            linearLayout.addView(respostaTextView)

            if (index < listaUbsBairros.size - 1) {
                val separator = View(this)
                val separatorLayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                )
                separatorLayoutParams.setMargins(0, marginInDp, 0, marginInDp)
                separator.layoutParams = separatorLayoutParams
                separator.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                linearLayout.addView(separator)
            }
        }
        scrollView.addView(linearLayout)

        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Confira a UBS mais próxima de você")
        builder.setView(scrollView)
        builder.setPositiveButton("Fechar", null)

        val dialog = builder.create()
        dialog.show()
    }
    private fun exibirListaPerguntasFrequentes() {
        val faqItems = listOf(
            FaqItem(
                "O que é o SUS?",
                "O Sistema Único de Saúde (SUS) é um sistema público de saúde no Brasil que oferece serviços de saúde para todos os cidadãos, de forma gratuita."
            ), FaqItem(
                "Como posso encontrar unidades de saúde próximas a mim?",
                "Você pode usar o aplicativo para encontrar unidades de saúde próximas à sua localização atual. Basta digitar seus sintomas e o aplicativo mostrará as unidades mais próximas que podem atendê-lo."
            ), FaqItem(
                "Posso agendar consultas pelo aplicativo?",
                "Não, o aplicativo não oferece a funcionalidade de agendamento de consultas. Ele fornece informações sobre as unidades de saúde e suas localizações."
            ), FaqItem(
                "Como posso obter informações sobre sintomas?",
                "O aplicativo fornece informações sobre sintomas com base em dados fornecidos pela Secretaria de Saúde. Você pode pesquisar sintomas para obter orientações sobre onde buscar atendimento."
            ), FaqItem(
                "Existe algum custo para usar o aplicativo?",
                "Não, o aplicativo é gratuito para todos os usuários. Ele visa fornecer informações e orientações sobre serviços de saúde do SUS."
            )
            // Adicione mais perguntas e respostas conforme necessário
        )

        val scrollView = ScrollView(this)
        scrollView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(20, 20, 20, 20)

        val marginInDp = 15

        for ((index, faqItem) in faqItems.withIndex()) {
            val perguntaTextView = TextView(this)
            perguntaTextView.text = faqItem.pergunta
            perguntaTextView.textSize = 18f
            linearLayout.addView(perguntaTextView)

            val respostaTextView = TextView(this)
            respostaTextView.text = faqItem.resposta
            respostaTextView.textSize = 14f
            respostaTextView.setTextColor(resources.getColor(android.R.color.darker_gray))
            respostaTextView.setPadding(0, 0, 0, 20)
            linearLayout.addView(respostaTextView)

            if (index < faqItems.size - 1) {
                val separator = View(this)
                val separatorLayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1 // Altura da linha de separação em dp
                )
                separatorLayoutParams.setMargins(0, marginInDp, 0, marginInDp)
                separator.layoutParams = separatorLayoutParams
                separator.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                linearLayout.addView(separator)
            }
        }

        scrollView.addView(linearLayout)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Perguntas Frequentes")
        builder.setView(scrollView)
        builder.setPositiveButton("Fechar", null)

        val dialog = builder.create()
        dialog.show()
    }

    data class FaqItem(val pergunta: String, val resposta: String)

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun obterLocalizacaoAtual() {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val locInicial = LatLng(location.latitude, location.longitude)
                val markerIcon =
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                googleMap.addMarker(
                    MarkerOptions().position(locInicial).title("Sua Localização").icon(markerIcon)
                )
                val zoomLevel = 14f
                zoomLevel
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        locInicial, zoomLevel
                    )
                )
            } else {
                exibirMensagemErro("Não foi possível obter a sua localização. Ative a localização do dispositivo e tente novamente.")

            }
        }.addOnFailureListener { e ->
            exibirMensagemErro("Falha na obtenção da localização. Verifique as configurações de localização do dispositivo.")
        }
    }

    private fun exibirMensagemErro(mensagem: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, "Erro: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, obter a localização atual
                obterLocalizacaoAtual()
            } else {
                // Permissão negada, tratar conforme necessário
                // Por exemplo, exibir uma mensagem ao usuário
            }
        }
    }


    private fun msgNenhumResultadoEncontrado() {

        runOnUiThread {
            Toast.makeText(this@MainActivity, "Nenhum local encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tamanhoLayoutBusca(tamanho:Int){
        val bgMenuSearch = findViewById<ImageView>(R.id.bgMenuSearch)
        val layoutParams = bgMenuSearch.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = tamanho
        bgMenuSearch.layoutParams = layoutParams
    }

    fun limparInput(input: EditText) {
        input.text.clear()
        limparTudo()
        tamanhoLayoutBusca(-400)

    }

    private fun runOnUiThread(context: Context, textoBusca: String, userLocation: LatLng) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://thaianramalho.com/api_belizario/atendimento.php?senha=dxic5CyB&sintoma=${textoBusca}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showErrorDialog(context)
            }

            @SuppressLint("CutPasteId")
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                if (responseData == "Nenhum resultado encontrado.") {
                    runOnUiThread {
                        msgNenhumResultadoEncontrado()
                        val inputBusca = findViewById<EditText>(R.id.inputBusca)
                        limparInput(inputBusca)
                        limparTudo()

                    }
                } else {
                    responseData?.let {
                        val jsonArray = JSONArray(it)
                        val localizacoesApi =
                            Gson().fromJson(it, Array<LocalizacaoApi>::class.java).toList()


                        runOnUiThread {
                            val layout = findViewById<LinearLayout>(R.id.layoutPrincipal)
                            layout.removeAllViews()

                            for (marker in markerList) {
                                marker?.remove()
                            }
                            markerList.clear()

                            val listaLocais = findViewById<TextView>(R.id.listaLocais)
                            listaLocais?.text = ""
                            listaLocais?.visibility = View.VISIBLE


                            localizacoesApi.forEach { localizacao ->
                                val textView = TextView(this@MainActivity)
                                val params = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, 16, 0, 0)
                                textView.layoutParams = params

                                val text = "<br/><b>${localizacao.nome}</b><br/>"
                                textView.text = HtmlCompat.fromHtml(
                                    text, HtmlCompat.FROM_HTML_MODE_COMPACT
                                )

                                textView.setBackgroundResource(R.drawable.rounded_background)

                                textView.setTextColor(
                                    ContextCompat.getColor(
                                        this@MainActivity, android.R.color.darker_gray
                                    )
                                )

                                textView.textSize = 16f

                                textView.setPadding(10, 10, 10, 10)

                                textView.gravity = Gravity.CENTER

                                textView.setOnClickListener {
                                    val latLngArray = localizacao.latlng.split(",")
                                    if (latLngArray.size == 2) {
                                        val latitude = latLngArray[0].toDouble()
                                        val longitude = latLngArray[1].toDouble()
                                        val loc = LatLng(latitude, longitude)
                                        val zoomLevel = 16f
                                        googleMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                loc, zoomLevel
                                            )
                                        )
                                    }
                                }

                                val layout = findViewById<LinearLayout>(R.id.layoutPrincipal)
                                layout.addView(textView)
                            }






                            localizacoesApi.forEach { localizacao ->
                                val latLngArray = localizacao.latlng.split(",")
                                if (latLngArray.size == 2) {
                                    val latitude = latLngArray[0].toDouble()
                                    val longitude = latLngArray[1].toDouble()
                                    val locAtendimento = LatLng(latitude, longitude)

                                    val distancia = calcularDistancia(userLocation, locAtendimento)

                                    if (markerMaisProximo == null || distancia < distanciaMaisProxima) {
                                        markerMaisProximo = locAtendimento
                                        distanciaMaisProxima = distancia
                                    }

                                    val marker = googleMap.addMarker(
                                        MarkerOptions().position(locAtendimento)
                                            .title(localizacao.nome)
                                    )

                                    marker?.tag = localizacao

                                    googleMap.setOnInfoWindowClickListener { clickedMarker ->
                                        val localizacaoClicada =
                                            clickedMarker.tag as? LocalizacaoApi

                                        if (localizacaoClicada != null) {
                                            val latLngArray = localizacaoClicada.latlng.split(",")
                                            if (latLngArray.size == 2) {
                                                val latitude = latLngArray[0].toDouble()
                                                val longitude = latLngArray[1].toDouble()
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${localizacaoClicada.nome})")
                                                )
                                                startActivity(intent)
                                            }
                                        }
                                    }


                                    if (marker != null) {
                                        markerList.add(marker)
                                    } else {
                                        markerList.clear()
                                    }
                                }
                            }

                            markerMaisProximo?.let {
                                val zoomLevel = 16f
                                googleMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        it, zoomLevel
                                    )
                                )
                            }

                        }
                    }
                }
            }
        })
    }

    private fun limparTudo() {

        for (marker in markerList) {
            marker?.remove()
        }
        markerList.clear()

        val layout = findViewById<LinearLayout>(R.id.layoutPrincipal)
        layout.removeAllViews()
    }

    private fun calcularDistancia(location1: LatLng, location2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            location1.latitude,
            location1.longitude,
            location2.latitude,
            location2.longitude,
            results
        )
        return results[0]
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
