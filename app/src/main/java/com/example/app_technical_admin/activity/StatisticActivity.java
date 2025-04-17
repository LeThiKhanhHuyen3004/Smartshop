package com.example.app_technical_admin.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app_technical_admin.R;
import com.example.app_technical_admin.adapter.PhoneAdapter;
import com.example.app_technical_admin.retrofit.ApiSale;
import com.example.app_technical_admin.retrofit.RetrofitClient;
import com.example.app_technical_admin.utils.Utils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class StatisticActivity extends AppCompatActivity {
    Toolbar toolbar;
    PieChart pieChart;
    BarChart barChart;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiSale apiSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistic);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        apiSale = RetrofitClient.getInstance(Utils.BASE_URL).create(ApiSale.class);
        initView();
        getdataChart();
        ActionToolBar();
        settingBarchart();



    }

    private void settingBarchart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawValueAboveBar(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setAxisMinimum(1);
        xAxis.setAxisMaximum(12);
        YAxis yAxisRight = barChart.getAxisRight();
        yAxisRight.setAxisMinimum(0);
        YAxis yAxisLeft = barChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_statistic, menu);
        Log.d("Menu ID", "monthStatistic ID: " + R.id.monthStatistic);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.monthStatistic) {
            getMonthStatistic();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void getMonthStatistic() {
        barChart.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.GONE);
        compositeDisposable.add(apiSale.getMonthStatistic()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                       statisticModel -> {
                           if(statisticModel.isSuccess()){
                               List<BarEntry> listdata = new ArrayList<>();
                               for(int i = 0; i<statisticModel.getResult().size(); i++){
                                   String total = statisticModel.getResult().get(i).getMonthTotal();
                                   String month = statisticModel.getResult().get(i).getMonth();
                                   listdata.add(new BarEntry(Integer.parseInt(month), Float.parseFloat(total)));
                               }
                               BarDataSet barDataSet = new BarDataSet(listdata, "Statistic");
                               barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                               barDataSet.setValueTextSize(14f);
                               barDataSet.setValueTextColor(Color.RED);

                               BarData data = new BarData(barDataSet);
                               barChart.animateXY(2000, 2000);
                               barChart.setData(data);
                               barChart.invalidate();

                           }



                       } ,
                        throwable -> {

                        }
                ));
    }

    private void getdataChart() {
        List<PieEntry> listdata = new ArrayList<>();
        compositeDisposable.add(apiSale.getStatistic()
                .subscribeOn(Schedulers.io()) // Chạy tác vụ API trên luồng nền
                .observeOn(AndroidSchedulers.mainThread()) // Cập nhật UI trên luồng chính
                .subscribe(
                        statisticModel -> {
                            if(statisticModel.isSuccess()){
                                for(int i = 0; i< statisticModel.getResult().size(); i++){
                                    String productName = statisticModel.getResult().get(i).getProductName();
                                    int total = statisticModel.getResult().get(i).getTotal();
                                    listdata.add(new PieEntry(total, productName));
                                }
                                PieDataSet pieDataSet = new PieDataSet(listdata, "Statistic");
                                PieData data = new PieData();
                                data.setDataSet(pieDataSet);
                                data.setValueTextSize(12f);
                                data.setValueFormatter(new PercentFormatter(pieChart));
                                pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                                pieChart.setData(data);
                                pieChart.animateXY(2000, 2000);
                                pieChart.setUsePercentValues(true);
                                pieChart.getDescription().setEnabled(false);
                                pieChart.invalidate();

                            }
                        },
                        throwable -> {
                            Log.d("log", throwable.getMessage());
                        }
                ));
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        pieChart = findViewById(R.id.piechart);
        barChart = findViewById(R.id.barchart);
    }

    private void ActionToolBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}