package com.example.cambio

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cambio.ui.theme.CambioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CambioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyConverterApp()
                }
            }
        }
    }
}
// Modelo de dados para a cotação
data class Cotacao(val moeda: String, val valor: String, val data: String)

@Composable
fun CurrencyConverterApp() {
    var cotacoes by remember { mutableStateOf<List<Cotacao>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Carrega os dados quando a tela inicia
    LaunchedEffect(Unit) {
        cotacoes = withContext(Dispatchers.IO) { buscarCotacoes() }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Imagem de dinheiro
            Image(
                painter = painterResource(id = R.drawable.ic_money), // Substitua por sua imagem
                contentDescription = stringResource(id = R.string.money_image_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
                // Campo para inserção do valor
                var inputValue by remember { mutableStateOf("") }
                BasicTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputValue.isEmpty()) Text(
                                stringResource(id = R.string.enter_value_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            innerTextField()
                        }
                    }
               )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de seleção de conversão

            val conversionOptions = listOf(
                stringResource(id = R.string.euro_to_real),
                stringResource(id = R.string.real_to_euro),
                stringResource(id = R.string.dollar_to_real),
                stringResource(id = R.string.dollar_to_euro)
            )
            val locale = Resources.getSystem().configuration.locales[0].language
            val menuColor = if (locale == "pt") MaterialTheme.colorScheme.secondaryContainer else Color.Magenta
            var selectedConversion by remember { mutableStateOf(conversionOptions.first()) }
            DropdownMenuField(
                options = conversionOptions,
                selectedOption = selectedConversion,
                onOptionSelected = { selectedConversion = it },
                colorMenu=menuColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para exibição do resultado da conversão
            Text(
                text = stringResource(id = R.string.result_text, (inputValue.toDoubleOrNull() ?: 0.0) * 5.0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de últimas cotações
            Text(
                text = stringResource(id = R.string.latest_quotes_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cotacoes) { cotacao ->
                    Text(
                        text = "${cotacao.moeda}: ${cotacao.valor} (${cotacao.data})",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }


// Função suspensa que simula a busca de cotações
suspend fun buscarCotacoes(): List<Cotacao> {

    return listOf(
        Cotacao("Euro", "6.55", "09/02/2025"),
        Cotacao("Euro", "6.58", "08/02/2025"),
        Cotacao("Euro", "6.50", "07/02/2025"),
        Cotacao("Dólar", "6.10", "09/02/2025"),
        Cotacao("Dólar", "6.06", "08/02/2025"),
        buscarCotacaoDolar("07/02/2025")
    )
}

//Recuperando a cotação do dia 07/02/2025
suspend fun buscarCotacaoDolar(data: String): Cotacao {
    val response:String? =mLoad("https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarDia(dataCotacao=@dataCotacao)?@dataCotacao=%2702-07-2025%27&\$top=100&\$format=json&\$select=cotacaoCompra,dataHoraCotacao")?.readText()
    Log.v("Retorno:","retorno:"+response)
    return Cotacao("Dólar", "5,75", data)

}
suspend fun mLoad(string: String): BufferedReader? {
    val url: URL = mStringToURL(string)!!
    val connection: HttpsURLConnection?
    try {
        connection = url.openConnection() as HttpsURLConnection
        connection.requestMethod= "GET"
        connection.connectTimeout= 20000
        connection.connect()

        Log.v("PDM", "Response Code: "+connection.responseCode)
        Log.v("PDM", "Response: "+connection.responseMessage)

        val inputStream: InputStream = connection.inputStream
        val bufferedInputStream = BufferedInputStream(inputStream)
        return bufferedInputStream.bufferedReader(Charsets.UTF_8)
    } catch (e: IOException) {
        e.printStackTrace()
        Log.v("PDM", "Erro de comunicação: "+e.message)

    }
    return null
}
// Function to convert string to URL
private fun mStringToURL(string: String): URL? {
    try {
        return URL(string)
    } catch (e: MalformedURLException) {
        e.printStackTrace()
        Log.v("PDM", "Erro de formatação da URL: "+e.message)
    }
    return null
}
@Composable
fun DropdownMenuField(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    colorMenu: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }) {
            Text(text = selectedOption)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colorMenu)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    text = { Text(option) }
                )
            }
        }
    }
}
