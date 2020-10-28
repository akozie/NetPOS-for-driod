package com.woleapp.netpos.ui.fragments

import androidx.fragment.app.Fragment
import com.woleapp.netpos.R

open class BaseFragment : Fragment() {

    fun addFragmentWithoutRemove(
        fragment: Fragment,
        containerViewId: Int = R.id.container_main,
        fragmentName: String? = null
    ) {
        val tag = fragment.javaClass.simpleName
        requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(
                R.anim.right_to_left, R.anim.left_to_right,
                R.anim.right_to_left, R.anim.left_to_right
            )
            add(containerViewId, fragment, fragmentName)
            addToBackStack(tag)
        }.commit()
    }

    open fun showFragment(targetFragment: Fragment?, className: String?) {
        val ft = requireActivity().supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(R.anim.right_to_left, R.anim.left_to_right)
        ft.replace(R.id.container_main, targetFragment!!, className)
        ft.commitAllowingStateLoss()
    }
}