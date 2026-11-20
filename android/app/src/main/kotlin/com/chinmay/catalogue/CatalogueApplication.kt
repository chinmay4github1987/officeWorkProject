package com.chinmay.catalogue

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt builds the dependency graph from here. */
@HiltAndroidApp
class CatalogueApplication : Application()
