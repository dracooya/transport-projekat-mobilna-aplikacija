package com.example.uberapp_tim9.passenger.ride_history;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uberapp_tim9.R;
import com.example.uberapp_tim9.model.Ride;
import com.example.uberapp_tim9.model.dtos.RideCreatedDTO;
import com.example.uberapp_tim9.model.dtos.RidePageDTO;
import com.example.uberapp_tim9.model.enumerations.RideStatus;
import com.example.uberapp_tim9.passenger.ride_history.adapters.PassengerRidesAdapter;
import com.example.uberapp_tim9.shared.LoggedUserInfo;
import com.example.uberapp_tim9.shared.rest.RestApiManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PassengerRideHistoryFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_passenger_ride_history, container, false);
        RecyclerView list = view.findViewById(R.id.rides_list);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        list.setLayoutManager(llm);

        final List<Ride>[] rides = new List[]{new ArrayList<>()};
        Call<ResponseBody> ridesCall = RestApiManager.restApiInterfacePassenger.getPassengersRides(LoggedUserInfo.id);
        ridesCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                                throws JsonParseException {
                            return LocalDateTime.parse(json.getAsString());
                        }
                    }).create();

                    RidePageDTO val = gson.fromJson(response.body().string(), new TypeToken<RidePageDTO>() {}.getType());
                    for (RideCreatedDTO ride : val.getResults()) {
                        if (ride.getStatus().equals(RideStatus.FINISHED)) {
                            rides[0].add(new Ride(ride));
                        }
                    }

                    rides[0].sort(Comparator.comparing(Ride::getmStartTime));

                    PassengerRidesAdapter adapter = new PassengerRidesAdapter(rides[0]);
                    list.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("RideHistory", "Failed to get rides");
            }
        });

        PassengerRidesAdapter adapter = new PassengerRidesAdapter(PassengerRideHistoryMockupData.getRides());

        list.setAdapter(adapter);

        return view;
    }
}