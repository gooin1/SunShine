package xyz.gooin.sunshine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        ArrayAdapter<String> mForecastAdapter;

        public PlaceholderFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // 创建一些示例数据. 此处显示示例天气预报
            String[] data = {
                    "10/12 - 晴 - 真tm冷",
                    "10/13 - 阴 - 贼冷",
                    "10/14 - 晴 - hot",
                    "Fri 6/27 - Foggy - 21/10",
                    "10/15 - 晴 - rain",
                    "10/16 - 晴 - 穿了衣服和没穿一样",
                    "10/17 - 晴 - 暴风呼啸"
            };

            List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));


            //  新建一个 Adapter 从资源中获取数据(比如新建的示例数据)
            //  用 Adapter 来向 ListView 中添加数据
            mForecastAdapter = new ArrayAdapter<String>(
                    getActivity(),                      //
                    R.layout.list_item_forecast,        // layout的名字
                    R.id.list_item_forecast_textview,   // textview的 id
                    weekForecast);                      // 传入的数据(使用上面的示例数据)


            ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
            listView.setAdapter(mForecastAdapter);
            Log.i(TAG, "onCreateView: ListView");

            return rootView;


        }
    }

}
