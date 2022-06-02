package com.woleapp.netpos.contactless.viewmodels

import androidx.lifecycle.ViewModel
import com.woleapp.netpos.contactless.database.dao.TransactionResponseDao


class DashBoardViewModel : ViewModel() {
    private lateinit var transactionResponseDao: TransactionResponseDao

    fun setDao(dao: TransactionResponseDao) {
        this.transactionResponseDao = dao
    }

}