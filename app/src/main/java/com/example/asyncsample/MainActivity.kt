package com.example.asyncsample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object{
        private const val DEBUG_TAG = "MSD"
        private const val WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?lang=ja"
        private const val APP_ID="af2bab7a274f898153923f019e2b3851"
    }
    private var _list:MutableList<MutableMap<String,String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _list = createList()
        val lvCityList = findViewById<ListView>(R.id.lvCityList)
        val from = arrayOf("name","q")
        val to= intArrayOf(android.R.id.text1,android.R.id.text2)
        val adapter = SimpleAdapter(this@MainActivity,_list,android.R.layout.simple_list_item_2,from,to)
        lvCityList.adapter=adapter
        lvCityList.onItemClickListener=listItemClickListener()
    }

    private fun createList():MutableList<MutableMap<String,String>>{
        val list:MutableList<MutableMap<String,String>> = mutableListOf()
        var city = mutableMapOf("name" to "大阪","q" to "Osaka")
        list.add(city)
        city = mutableMapOf("name" to "東京","q" to "Tokyo")
        list.add(city)
        city = mutableMapOf("name" to "神戸","q" to "Kobe")
        list.add(city)
        city = mutableMapOf("name" to "横浜","q" to "Yokohama")
        list.add(city)

        return list
    }
    private inner class listItemClickListener:AdapterView.OnItemClickListener{
        override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            //TODO("Not yet implemented")
            val item = _list.get(p2)
            val temp = item.get("name")
            Log.d("MSD","message:"+temp)

            val q = item.get("q")
            q?.let{
                val urlFull="$WEATHERINFO_URL&q=$q&appid=$APP_ID"
                Log.d("MSD","url:"+urlFull)
                receiveWeatherInfo(urlFull)
            }
        }
    }

    @UiThread
    private fun receiveWeatherInfo(urlFull:String){
        val backgroundReceiver = WeatherInfoBackgroundReceiver(urlFull)
        val executeService = Executors.newSingleThreadExecutor()
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
        Log.d("MSD","result:"+result)
        showWeatherInfo(result)

    }
    private inner class WeatherInfoBackgroundReceiver(url:String):Callable<String> {
        private val _url=url


        @WorkerThread
        override fun call(): String {
            //TODO("Not yet implemented")
            var result=""
            val url=URL(_url)
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout=1000
            con.readTimeout=1000
            con.requestMethod="GET"
            try{
                con.connect()
                val stream = con.inputStream
                result = is2String(stream)
                stream.close()
            }
            catch (ex : SocketTimeoutException){
                Log.d("MSD","TimeOut..",ex)
            }
            con.disconnect()

            return result
        }
    }
    private fun is2String(stream:InputStream):String{
        val sb= StringBuilder()
        val reader = BufferedReader(InputStreamReader(stream,StandardCharsets.UTF_8))
        var line = reader.readLine()

        while(line != null){
            sb.append(line)
            line = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }
    @UiThread
    private fun showWeatherInfo(result: String){
        val rootJSON = JSONObject(result)
        val cityName = rootJSON.getString("name")

        val coordJSON = rootJSON.getJSONObject("coord")
        val latitude = coordJSON.getString("lat")
        val longitude = coordJSON.getString("lon")

        val weatherJSONArray = rootJSON.optJSONArray("weather")
        val weatherJSON=weatherJSONArray.getJSONObject(0)
        val weather=weatherJSON.getString("description")
        val telop = "${cityName}の天気"
        val desc = "現在の天気は${weather}です。　\n緯度:経度＝${latitude} : ${longitude}です。"
        val tvweatherTelop = findViewById<TextView>(R.id.tvWeatherTelop)
        val tvweatherDesc = findViewById<TextView>(R.id.tvWeatherDesc)
        tvweatherTelop.text=telop
        tvweatherDesc.text=desc

    }
}