package com.woleapp.netpos.contactless.ui.fragments

import androidx.fragment.app.Fragment
import com.woleapp.netpos.contactless.R

open class BaseFragment : Fragment() {

    fun addFragmentWithoutRemove(
        fragment: Fragment,
        containerViewId: Int = R.id.container_main,
        fragmentName: String? = null
    ) {
        val tag = fragment.javaClass.simpleName
        requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.right_to_left,
                R.anim.left_to_right,
                R.anim.right_to_left,
                R.anim.left_to_right
            )
            add(containerViewId, fragment, fragmentName)
            addToBackStack(tag)
        }.commit()
    }

    open fun showFragment(
        fragment: Fragment,
        containerViewId: Int = R.id.container_main,
        fragmentName: String? = null
    ) {
        val tag = fragment.javaClass.simpleName
        requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.right_to_left,
                R.anim.left_to_right,
                R.anim.right_to_left,
                R.anim.left_to_right
            )
            replace(containerViewId, fragment, fragmentName)
            addToBackStack(tag)
        }.commit()
    }
}
