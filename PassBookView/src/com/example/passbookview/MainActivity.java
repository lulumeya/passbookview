package com.example.passbookview;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {

	private final List<OnBackPressedListener> listeners = new ArrayList<OnBackPressedListener>();

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		if ( savedInstanceState == null ) {
			getFragmentManager().beginTransaction().add( R.id.container, new PlaceholderFragment() ).commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
			View rootView = inflater.inflate( R.layout.fragment_main, container, false );
			return rootView;
		}
	}

	public void addOnBackPressedListener( OnBackPressedListener onBackPressedListener ) {
		if ( this.listeners.indexOf( onBackPressedListener ) == -1 ) {
			this.listeners.add( onBackPressedListener );
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.listeners.clear();
	}

	@Override
	public void onBackPressed() {
		if ( this.listeners.size() > 0 ) {
			for ( OnBackPressedListener item : this.listeners ) {
				if ( item.onBackPressed() ) {
					return;
				}
			}
		}
		super.onBackPressed();
	}
}