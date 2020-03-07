package com.tughi.aggregator.activities.tagsettings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Tags
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeleteTagDialogFragment : DialogFragment() {

    companion object {
        const val ARG_TAG_ID = "tag_id"
        const val ARG_TAG_NAME = "tag_title"
        const val ARG_FINISH_ACTIVITY = "finish_activity"

        fun show(fragmentManager: FragmentManager, tagId: Long, tagName: String, finishActivity: Boolean) {
            DeleteTagDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putLong(ARG_TAG_ID, tagId)
                            putString(ARG_TAG_NAME, tagName)
                            putBoolean(ARG_FINISH_ACTIVITY, finishActivity)
                        }
                    }
                    .show(fragmentManager, "delete-tag-dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val tagId = arguments.getLong(ARG_TAG_ID)
        val tagName = arguments.getString(ARG_TAG_NAME)
        val finishActivity = arguments.getBoolean(ARG_FINISH_ACTIVITY)
        return AlertDialog.Builder(requireContext())
                .setTitle(tagName)
                .setMessage(R.string.delete_tag__message)
                .setNegativeButton(R.string.action__no, null)
                .setPositiveButton(R.string.action__yes) { _, _ ->
                    GlobalScope.launch {
                        Tags.delete(Tags.DeleteTagCriteria(tagId))
                    }
                    if (finishActivity) {
                        activity?.finish()
                    }
                }
                .create()
    }

}
