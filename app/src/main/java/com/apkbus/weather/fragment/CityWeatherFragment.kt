package com.apkbus.weather.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.apkbus.weather.R
import com.apkbus.weather.activity.ChooseLocationActivity
import com.apkbus.weather.api.ApiCallBack
import com.apkbus.weather.api.ApiHelper
import com.apkbus.weather.entry.IndexBean
import com.apkbus.weather.entry.WeatherBean
import com.apkbus.weather.sharedPreference.WeatherSpKey
import com.apkbus.weather.utils.GsonUtils
import com.apkbus.weather.utils.getWeatherDataSp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.fragment_city_weather.*

class CityWeatherFragment : Fragment() {
    private val REQUSET_SELECT_CITY = 1101
    private var province: String? = ""
    private var city: String? = ""
    private var town: String? = ""
    private var mActivity = this.activity

    private val TAG_API = "ApiCallBack:"
    private var rootView: View? = null
    private var currWeatherDetail = ArrayList<IndexBean>()
    private var tenDayWeatherDetail = ArrayList<WeatherBean.ResultBean.FutureBean>()
    private var weatherBean: WeatherBean? = null
    private var gridAdapter = MyGridViewAdapter(R.layout.item_weather_index, currWeatherDetail)
    private var recyclerAdapter = MyRecyclerViewAdapter(R.layout.item_weather_prediction, tenDayWeatherDetail)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        city = arguments.getString("city")
        province = arguments.getString("province")
        town = arguments.getString("town")
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater?.inflate(R.layout.fragment_city_weather, container, false)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        address.setOnClickListener({
            val intent = Intent(this.activity, ChooseLocationActivity::class.java)
            this.activity.startActivityForResult(intent, REQUSET_SELECT_CITY)
        })
        when{
            TextUtils.isEmpty(province) -> province = "江苏"
            TextUtils.isEmpty(city) -> city = "苏州"
            TextUtils.isEmpty(town) -> town = "吴中"
        }
        onRefresh()
        grid?.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false)
        grid?.adapter = gridAdapter
        recycler?.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false)
        recycler.isNestedScrollingEnabled = false
        recycler?.adapter = recyclerAdapter
    }

    private fun onRefresh() {
        if (!TextUtils.isEmpty(getWeatherDataSp().getString(WeatherSpKey.data, ""))) {
            dealWeatherJson(getWeatherDataSp().getString(WeatherSpKey.data, ""), province, city, town)
        } else {
            getWeatherDetail(province, city, town)
        }
    }

    private fun getWeatherDetail(province: String?, city: String?, town: String?) {
        ApiHelper.getWeatherDetail(this.activity, province, city, town, object : ApiCallBack {
            override fun onSuccess(result: String) {
                getWeatherDataSp().edit().putString(WeatherSpKey.data, result).apply()
                dealWeatherJson(result, province, city, town)
            }

            override fun onError(msg: String) {
                Log.e(TAG_API, msg)
            }
        })
    }

    private fun dealWeatherJson(result: String, province: String?, city: String?, town: String?) {
        if (!TextUtils.isEmpty(result)) {
            weatherBean = GsonUtils.jsonToClass(result, WeatherBean::class.java)
            if (weatherBean?.result?.get(0)?.future != null && weatherBean?.result?.get(0)?.future!!.isNotEmpty()) {
                tenDayWeatherDetail.clear()
                tenDayWeatherDetail.addAll(weatherBean?.result?.get(0)?.future!!)
                recyclerAdapter.notifyDataSetChanged()
            }
            if (!TextUtils.isEmpty(town)) {
                toText(address, "$province—$city—$town")
            } else {
                toText(address, "哦呦少林郎，木有地址爪子查天气嘛")
            }
            toText(big_temperature, weatherBean?.result?.get(0)?.temperature)
            if (weatherBean?.result?.get(0)?.future?.size != 0 && weatherBean?.result?.get(0)?.future != null)
                tv_count.text = "·未来" + weatherBean?.result?.get(0)?.future?.size + "日天气预报"
            else
                tv_count.text = "天气预报跑丢了？没关系，所有的东西都跑丢了呢T_T"
            if (TextUtils.isEmpty(weatherBean?.result?.get(0)?.weather))
                weather.text = "天气情况：走失ing"
            else
                weather.text = "天气情况：" + weatherBean?.result?.get(0)?.weather
            if (TextUtils.isEmpty(weatherBean?.result?.get(0)?.weather))
                airCondition.text = "空气质量：走失ing"
            else
                airCondition.text = "空气质量：" + weatherBean?.result?.get(0)?.airCondition
            currWeatherDetail.clear()
            currWeatherDetail.add(0, IndexBean("·风向风力·", weatherBean?.result?.get(0)?.wind))
            currWeatherDetail.add(1, IndexBean("·日出时间·", weatherBean?.result?.get(0)?.sunrise))
            currWeatherDetail.add(2, IndexBean("·日落时间·", weatherBean?.result?.get(0)?.sunset))
            currWeatherDetail.add(3, IndexBean("·锻炼指数·", weatherBean?.result?.get(0)?.exerciseIndex))
            currWeatherDetail.add(4, IndexBean("·穿衣指数·", weatherBean?.result?.get(0)?.dressingIndex))
            currWeatherDetail.add(5, IndexBean("·洗衣指数·", weatherBean?.result?.get(0)?.washIndex))
            gridAdapter.notifyDataSetChanged()
        }
    }

    class MyGridViewAdapter(layoutRes: Int, datas: List<IndexBean>?) :
            BaseQuickAdapter<IndexBean, BaseViewHolder>(layoutRes, datas) {
        override fun convert(viewHolder: BaseViewHolder?, item: IndexBean) {
            toText(viewHolder!!.getView(R.id.item_key), item.indexName)
            toText(viewHolder.getView(R.id.item_value), item.indexContent)
        }
    }

    class MyRecyclerViewAdapter(layoutRes: Int, datas: List<WeatherBean.ResultBean.FutureBean>?) :
            BaseQuickAdapter<WeatherBean.ResultBean.FutureBean, BaseViewHolder>(layoutRes, datas) {
        override fun convert(viewHolder: BaseViewHolder?, item: WeatherBean.ResultBean.FutureBean) {
            toText(viewHolder!!.getView(R.id.date), item.date)
            toText(viewHolder.getView(R.id.dayTime), item.dayTime)
            toText(viewHolder.getView(R.id.night), item.night)
            toText(viewHolder.getView(R.id.temperature_section), item.temperature)
            toText(viewHolder.getView(R.id.wind), item.wind)
            toText(viewHolder.getView(R.id.week), item.week)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUSET_SELECT_CITY && resultCode == Activity.RESULT_OK && data != null) {
            val currCityName = data.getStringExtra("city")
            val currProvinceName = data.getStringExtra("province")
            val currTownName = data.getStringExtra("town")

            getWeatherDataSp().edit().putString(WeatherSpKey.provinceName, currProvinceName)

                    .putString(WeatherSpKey.cityName, currCityName).putString(WeatherSpKey.townName, currTownName).apply()
            getWeatherDetail(currProvinceName, currCityName, currTownName)

        }
    }

    companion object {
        // TextView非空赋值，空划线
        fun toText(text: TextView, str: String?) {
            if (TextUtils.isEmpty(str)) {
                text.text = "走失ing"
            } else {
                text.text = str
                text.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            }
        }
    }
}