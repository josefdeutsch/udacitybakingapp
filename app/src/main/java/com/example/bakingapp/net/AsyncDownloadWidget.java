package com.example.bakingapp.net;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.bakingapp.model.Ingredient;
import com.example.bakingapp.model.Recipe;
import com.example.bakingapp.ui.ErrorActivity;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.example.bakingapp.constant.Constants.BAKINGRECIPEBASEURL;


public class AsyncDownloadWidget extends AsyncTask<Void,Void, List<Ingredient>> {

    private Context mContext;
    private Integer mRecipeIndex;

    public AsyncDownloadWidget(Context context, Integer id){
        mContext = context;
        mRecipeIndex = id;
    }


    @Override
    protected List<Ingredient> doInBackground(Void... voids) {
        OkHttpClient okHttpClient = buildHttpClient();
        Retrofit retrofit = buildRetrofitClient(okHttpClient);
        Call<List<Recipe>> call = buildRetrofitServiceConnection(retrofit);
        return executeRetrofitServiceConnection(call);
    }


    private List<Ingredient> executeRetrofitServiceConnection(Call<List<Recipe>> call) {
        List<Ingredient> input = null;
        List<Ingredient> output = null;
        try {
            output = supplyInput(call, input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    private List<Ingredient> supplyInput(Call<List<Recipe>> call, List<Ingredient> input) throws IOException {
        int index = mRecipeIndex;
        input = call.execute().body().get(index).getIngredients();
        return input;
    }


    private Call<List<Recipe>> buildRetrofitServiceConnection(Retrofit retrofit) {
        RetrofitService service = retrofit.create(RetrofitService.class);
        return service.getPosts();
    }

    @NotNull
    private Retrofit buildRetrofitClient(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BAKINGRECIPEBASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }


    @NotNull
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // produces 408 request.code() - Timeout
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Response response = getResponseCode(chain);
                        return response;
                    }
                })
                .build();
    }


    @NotNull
    private Response getResponseCode(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        if (response.code() >= 301) {
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                     mContext.startActivity(new Intent(mContext, ErrorActivity.class));
                }
            };
        }
        return response;
    }
}
