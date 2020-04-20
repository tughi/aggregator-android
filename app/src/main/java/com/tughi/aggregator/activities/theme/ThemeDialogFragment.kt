package com.tughi.aggregator.activities.theme

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.tughi.aggregator.App
import com.tughi.aggregator.R

class ThemeDialogFragment : DialogFragment() {
    companion object {
        fun show(fragmentManager: FragmentManager) {
            ThemeDialogFragment().show(fragmentManager, "theme-dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()

        val view = activity.layoutInflater.inflate(R.layout.theme_dialog, FrameLayout(activity), false)

        App.style.observe(this, Observer {
            view.findViewById<ImageView>(R.id.theme_bottom_navigation_accent).setImageResource(when (it.accent) {
                App.Style.Accent.BLUE -> R.drawable.theme_accent_blue
                App.Style.Accent.GREEN -> R.drawable.theme_accent_green
                App.Style.Accent.ORANGE -> R.drawable.theme_accent_orange
                App.Style.Accent.PURPLE -> R.drawable.theme_accent_purple
                App.Style.Accent.RED -> R.drawable.theme_accent_red
            })
        })

        val onThemeClickListener = View.OnClickListener {
            when (it.id) {
                R.id.theme_accent_blue -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.BLUE)
                R.id.theme_accent_green -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.GREEN)
                R.id.theme_accent_orange -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.ORANGE)
                R.id.theme_accent_purple -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.PURPLE)
                R.id.theme_accent_red -> App.style.value = App.style.value?.copy(accent = App.Style.Accent.RED)
                R.id.theme_dark -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.DARK)
                R.id.theme_light -> App.style.value = App.style.value?.copy(theme = App.Style.Theme.LIGHT)
                R.id.theme_bottom_navigation_accent -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.ACCENT)
                R.id.theme_bottom_navigation_gray -> App.style.value = App.style.value?.copy(navigationBar = App.Style.NavigationBar.GRAY)
            }
        }

        view.findViewById<View>(R.id.theme_accent_blue).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_accent_green).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_accent_orange).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_accent_purple).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_accent_red).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_bottom_navigation_accent).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_bottom_navigation_gray).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_dark).setOnClickListener(onThemeClickListener)
        view.findViewById<View>(R.id.theme_light).setOnClickListener(onThemeClickListener)

        return AlertDialog.Builder(activity)
                .setView(view)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.finish()
        super.onDismiss(dialog)
    }
}
