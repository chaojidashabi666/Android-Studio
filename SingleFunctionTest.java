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

import java.util.Objects;

public class SingleFunctionTest extends AppCompatActivity
{
	
	@SuppressLint("StaticFieldLeak")
	protected static MainActivity Parent = null;    //用于调用MainActivity内函数的静态成员变量
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_single_function_test);
		
		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序 - 独立测试");
		
		if (Parent == null)
		{
			Toast.makeText(this, "Context is null!", Toast.LENGTH_SHORT).show();
			Log.e(Flags.SUB_ACTIVITY_TAG, "Context IS NULL");
			finish();
		}
		
		Parent.InitSingleFunctionTestUnit(this);
	}
}