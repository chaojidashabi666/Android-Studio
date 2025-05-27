/*
 * Copyright (c) 2023. UnknownNetworkService Group
 * This file is created by UnknownObject at 2023 - 5 - 1
 */

package com.qrs.maincarcontrolapp.gui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.qrs.maincarcontrolapp.R;
import com.qrs.maincarcontrolapp.constants.Flags;
import com.qrs.maincarcontrolapp.detect.EfficientnetTrafficLightNcnn;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLightNcnn;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLogoNcnn;

import java.util.Objects;

public class DebugPage extends AppCompatActivity
{
	@SuppressLint("StaticFieldLeak")
	protected static MainActivity Parent = null;    //用于调用MainActivity内函数的静态成员变量
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_debug_page);
		
		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序 - 调试");



		if (Parent == null)
		{
			Toast.makeText(this, "Context is null!", Toast.LENGTH_SHORT).show();
			Log.e(Flags.SUB_ACTIVITY_TAG, "Context IS NULL");
			finish();
		}
		
		findViewById(R.id.btn_debug_send).setOnClickListener(view -> Parent.DebugPageCallback_DebugSend());
		
		findViewById(R.id.btn_send).setOnClickListener(view -> Parent.DebugPageCallback_SendByInput(this));

		findViewById(R.id.trafficLight).setOnClickListener(view -> Parent.trafficLightResDebug(this));
	}
}