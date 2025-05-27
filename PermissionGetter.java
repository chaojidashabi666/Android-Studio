/*
 * Copyright (c) 2022. UnknownNetworkService Group
 * This file is created by UnknownObject at 2022 - 10 - 8
 */

package com.qrs.maincarcontrolapp.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.qrs.maincarcontrolapp.R;

import java.util.Objects;

//Android的存储权限的获取
public class PermissionGetter extends AppCompatActivity
{
	
	private final int privilege_request_code = 0x3012065;
	
	private void GetExternalStoragePrivilege()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			// 先判断有没有权限
			if (!Environment.isExternalStorageManager())
			{
				Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				intent.setData(Uri.parse("package:" + this.getPackageName()));
				startActivityForResult(intent, privilege_request_code);
			}
			else
			{
				setResult(RESULT_OK);
				finish();
			}
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			// 先判断有没有权限
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
					ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
			{
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, privilege_request_code);
			}
			else
			{
				setResult(RESULT_OK);
				finish();
			}
		}
		else
		{
			setResult(RESULT_OK);
			finish();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_permission_getter);
		
		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序 - 权限获取");
		
		GetExternalStoragePrivilege();
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == privilege_request_code)
		{
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
					ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
				setResult(RESULT_OK);
			else
				setResult(RESULT_CANCELED);
			finish();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == privilege_request_code && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
		{
			if (Environment.isExternalStorageManager())
				setResult(RESULT_OK);
			else
				setResult(RESULT_CANCELED);
			finish();
		}
	}
}