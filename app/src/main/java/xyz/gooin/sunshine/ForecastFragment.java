package xyz.gooin.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Created by gooin on 2016/10/14.
 */


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    public static final String API_KEY = "b4eee530b641f9cf598669268e3f7a9c";
    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    // 实例化选项菜单
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // 传入菜单文件的资源id,填充到 Menu 实例中
        inflater.inflate(R.menu.forecastfragment, menu);
        inflater.inflate(R.menu.detail, menu);

    }

    // 响应菜单的点击
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                updateWeather();
                return true;

            case R.id.action_settings:
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //  新建一个 Adapter 从资源中获取数据(比如新建的示例数据)
        //  用 Adapter 来向 ListView 中添加数据
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),                      //
                R.layout.list_item_forecast,        // layout的名字
                R.id.list_item_forecast_textview,   // textview的 id
                new ArrayList<String>());            // 传入的数据(使用上面的示例数据)


        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        // 向 listview 传入天气数据
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String forecast = mForecastAdapter.getItem(i);
                Toast.makeText(getActivity(),"hello", Toast.LENGTH_SHORT).show();
                // 使用显示 intent 打开DetailActivity
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        // 存入数据
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);

            }
        });
        return rootView;
    }

    // 传入参数更新 天气
    private void updateWeather() {
        // 执行后台线程:获取json
        FetchWeatherTask weatherTask = new FetchWeatherTask();
//                weatherTask.execute("73160");
        // 读取设置中更新的参数 : 获取邮政编码
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = preferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        Log.i(TAG, "onOptionsItemSelected: location :" + location);
        weatherTask.execute(location);

    }

    @Override
    public void onStart() {
        super.onStart();
        // 创建Fragment时更新天气数据
        updateWeather();
    }

    // 用 AsyncTask 新开一个线程 来访问网络
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {


        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low ,String unitType) {
            if (unitType.equals(getString(R.string.pref_temperature_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_temperature_units_metric))) {
                Log.d(TAG, " UNIT ERROR" + unitType);
            }





            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {

            // 要提取的 JSON 对象
            final String OWN_LIST = "list";
            final String OWN_WEATHER = "weather";
            final String OWN_TEMPERATURE = "temp";
            final String OWN_MAX = "max";
            final String OWN_MIN = "min";
            final String OWN_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWN_LIST);

            android.text.format.Time dayTime = new android.text.format.Time();
            dayTime.setToNow();

            int julanStartDay = android.text.format.Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new android.text.format.Time();

            String[] resultStrs = new String[numDays];

            // 从设置中获取设定的温度单位
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPreferences.getString(getString(R.string.pref_temperature_key),getString(R.string.pref_temperature_units_metric));


            for (int i = 0; i < weatherArray.length(); i++) {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;

                dateTime = dayTime.setJulianDay(julanStartDay + i);
                day = getReadableDateString(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWN_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWN_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWN_TEMPERATURE);
                double high = temperatureObject.getDouble(OWN_MAX);
                double low = temperatureObject.getDouble(OWN_MIN);

                highAndLow = formatHighLows(high, low ,unitType);

                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            for (String s : resultStrs
                    ) {
                Log.i(TAG, "getWeatherDataFromJson: " + s);

            }
            return resultStrs;
        }


        public FetchWeatherTask() {
            super();
        }

        @Override
        protected String[] doInBackground(String... params) {

            // 如果没有邮政编码,就没啥可查询的
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "MODE";
                final String UNIT_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                // 用 URi 构建查询网址
                Uri builtUrl = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNIT_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, API_KEY)
                        .build();

                URL url = new URL(builtUrl.toString());
                Log.i(TAG, "doInBackground: URL = " + url);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                // show json in logcat
//                Log.i(TAG, "doInBackground: JSON : " + forecastJsonStr);

            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(result);
            }
        }
    }


}
