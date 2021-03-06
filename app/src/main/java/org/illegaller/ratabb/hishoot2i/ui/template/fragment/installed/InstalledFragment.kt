package org.illegaller.ratabb.hishoot2i.ui.template.fragment.installed

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import common.ext.isVisible
import common.ext.preventMultipleClick
import org.illegaller.ratabb.hishoot2i.R
import org.illegaller.ratabb.hishoot2i.ui.template.SortTemplateDialog
import org.illegaller.ratabb.hishoot2i.ui.template.fragment.AbsTemplateFragment
import org.illegaller.ratabb.hishoot2i.ui.template.fragment.SwipeHelper
import template.Template
import timber.log.Timber
import javax.inject.Inject

class InstalledFragment : AbsTemplateFragment(), InstalledFragmentView {
    @Inject
    lateinit var presenter: InstalledFragmentPresenter
    private lateinit var templateRecyclerView: RecyclerView
    private lateinit var noContent: View
    private lateinit var loading: View
    override fun layoutRes(): Int = R.layout.fragment_template
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.clickItem = ::onClickItemAdapter
        templateRecyclerView = view.findViewById(R.id.templateRecyclerView)
        noContent = view.findViewById(R.id.noContent)
        loading = view.findViewById(R.id.loading)
        templateRecyclerView.adapter = adapter
        ItemTouchHelper(SwipeHelper(this)).attachToRecyclerView(templateRecyclerView)
        presenter.attachView(this)
        presenter.render()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        presenter.setUpMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_sort_template -> {
            item.preventMultipleClick {
                SortTemplateDialog().apply {
                    callback = {
                        isForceUpdateUI = true
                        presenter.render()
                    }
                }.show(childFragmentManager)
                true
            }
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    private fun onClickItemAdapter(template: Template) {
        if (presenter.setCurrentTemplate(template)) {
            Snackbar.make(
                templateRecyclerView,
                "Template: ${template.name}", // TODO:
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun setData(data: List<Template>) {
        if (data.isNotEmpty()) {
            val state = templateRecyclerView.layoutManager.onSaveInstanceState()
            adapter.submitList(data, isForceUpdateUI)
            isForceUpdateUI = false
            templateRecyclerView.layoutManager.onRestoreInstanceState(state)
            templateRecyclerView.isVisible = true
            noContent.isVisible = false
            //
            updateTabBadgeInstalled(data.size)
        } else {
            templateRecyclerView.isVisible = false
            noContent.isVisible = true
        }
    }

    override fun showProgress() {
        loading.isVisible = true
        templateRecyclerView.isVisible = false
    }

    override fun hideProgress() {
        loading.isVisible = false
    }

    override fun onError(e: Throwable) { //
        Timber.e(e)
        //   context?.toast(e.localizedMessage)
    }

    override fun onSwipedToLeft(position: Int) { // TODO: delete/ uninstall app template.
        val template = adapter.getItemAt(position)
        Timber.d("onSwipedToLeft ${template.name}")
        if (!tryUninstallTemplate(position, template)) {
            adapter.notifyItemChanged(position)
        }
    }

    override fun onSwipedToRight(position: Int) { // TODO: add/remove to fav
        val template = adapter.getItemAt(position)
        Timber.d("onSwipedToRight ${template.name}")
        template.let {
            presenter.addRemoveTemplateFav(it) { isRemove ->
                Timber.d("isRemove:$isRemove")
            }
        }
        adapter.notifyItemChanged(position)
    }
}