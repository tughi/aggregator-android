package com.tughi.aggregator.activities.entrytagrulesettings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.tughi.aggregator.R
import com.tughi.aggregator.data.DeleteEntryTagRuleCriteria
import com.tughi.aggregator.data.EntryTagRules
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DeleteEntryTagRuleDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ENTRY_TAG_RULE_ID = "entry_tag_rule_id"
        private const val ARG_FINISH_ACTIVITY = "finish_activity"

        fun show(fragmentManager: FragmentManager, entryTagRuleId: Long, finishActivity: Boolean) {
            DeleteEntryTagRuleDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putLong(ARG_ENTRY_TAG_RULE_ID, entryTagRuleId)
                            putBoolean(ARG_FINISH_ACTIVITY, finishActivity)
                        }
                    }
                    .show(fragmentManager, "delete-dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments!!
        val entryTagRuleId = arguments.getLong(ARG_ENTRY_TAG_RULE_ID)
        val finishActivity = arguments.getBoolean(ARG_FINISH_ACTIVITY)
        return AlertDialog.Builder(context!!)
                .setMessage(R.string.entry_tag_rule__delete__message)
                .setNegativeButton(R.string.action__no, null)
                .setPositiveButton(R.string.action__yes) { _, _ ->
                    GlobalScope.launch {
                        EntryTagRules.delete(DeleteEntryTagRuleCriteria(entryTagRuleId))
                    }
                    if (finishActivity) {
                        activity?.finish()
                    }
                }
                .create()
    }

}
