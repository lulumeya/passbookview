package com.example.passbookview;

public interface DataListener<T extends Object> {

	public void onFinish( T result );
}
