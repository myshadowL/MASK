package com.face.mask;


import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class AppCompatPreferenceActivity
        extends PreferenceActivity
{
    private AppCompatDelegate mDelegate;

    private AppCompatDelegate getDelegate()
    {
        if (this.mDelegate == null) {
            this.mDelegate = AppCompatDelegate.create(this, null);
        }
        return this.mDelegate;
    }

    public void addContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams)
    {
        getDelegate().addContentView(paramView, paramLayoutParams);
    }

    public MenuInflater getMenuInflater()
    {
        return getDelegate().getMenuInflater();
    }

    public ActionBar getSupportActionBar()
    {
        return getDelegate().getSupportActionBar();
    }

    public void invalidateOptionsMenu()
    {
        getDelegate().invalidateOptionsMenu();
    }

    public void onConfigurationChanged(Configuration paramConfiguration)
    {
        super.onConfigurationChanged(paramConfiguration);
        getDelegate().onConfigurationChanged(paramConfiguration);
    }

    protected void onCreate(Bundle paramBundle)
    {
        getDelegate().installViewFactory();
        getDelegate().onCreate(paramBundle);
        super.onCreate(paramBundle);
    }

    protected void onDestroy()
    {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    protected void onPostCreate(Bundle paramBundle)
    {
        super.onPostCreate(paramBundle);
        getDelegate().onPostCreate(paramBundle);
    }

    protected void onPostResume()
    {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    protected void onStop()
    {
        super.onStop();
        getDelegate().onStop();
    }

    protected void onTitleChanged(CharSequence paramCharSequence, int paramInt)
    {
        super.onTitleChanged(paramCharSequence, paramInt);
        getDelegate().setTitle(paramCharSequence);
    }

    public void setContentView(int paramInt)
    {
        getDelegate().setContentView(paramInt);
    }

    public void setContentView(View paramView)
    {
        getDelegate().setContentView(paramView);
    }

    public void setContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams)
    {
        getDelegate().setContentView(paramView, paramLayoutParams);
    }

    public void setSupportActionBar(Toolbar paramToolbar)
    {
        getDelegate().setSupportActionBar(paramToolbar);
    }
}

