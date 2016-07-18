package com.criptext.http;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by gesuwall on 6/16/16.
 */
public class LoggingInterceptor implements Interceptor {
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        Log.d("Okhttp", String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));
        Log.d("Okhttp", bodyToString(request.body()));

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        Log.d("Okhttp", String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));

        return response;
    }

    private static String bodyToString(final RequestBody request){
        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            copy.writeTo(buffer);
            return buffer.readUtf8();
        }
        catch (final IOException e) {
            return "did not work";
        }
    }
}
