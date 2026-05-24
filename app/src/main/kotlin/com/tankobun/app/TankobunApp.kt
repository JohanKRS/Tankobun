package com.tankobun.app

import androidx.compose.runtime.Composable
import com.tankobun.app.ui.shell.TankobunAppRoot

@Composable
fun TankobunApp(viewModel: MainViewModel) {
    TankobunAppRoot(viewModel)
}
