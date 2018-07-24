package com.adamnickle.reptrack.ui.post

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View
import com.adamnickle.reptrack.R
import com.adamnickle.reptrack.model.post.Post
import com.adamnickle.reptrack.model.post.PostDao
import com.adamnickle.reptrack.network.PostApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PostListViewModel @Inject constructor(
        private val postApi: PostApi,
        private val postDao: PostDao
): ViewModel()
{
    val loadingVisibility: MutableLiveData<Int> = MutableLiveData()
    val errorMessage: MutableLiveData<Int> = MutableLiveData()
    val errorClickListener = View.OnClickListener { loadPosts() }
    val postListAdapter: PostListAdapter = PostListAdapter()

    private lateinit var subscription: Disposable

    init
    {
        loadPosts()
    }

    override fun onCleared()
    {
        super.onCleared()

        subscription.dispose()
    }

    private fun loadPosts()
    {
        subscription = Observable.fromCallable { postDao.all }
                .concatMap { dbPostList ->
                    if( dbPostList.isEmpty() )
                    {
                        postApi.getPosts().concatMap { apiPostList ->
                            postDao.insertAll( *apiPostList.toTypedArray() )
                            Observable.just( apiPostList )
                        }
                    }
                    else
                    {
                        Observable.just( dbPostList )
                    }
                }
                .subscribeOn( Schedulers.io() )
                .observeOn( AndroidSchedulers.mainThread() )
                .doOnSubscribe { onRetrievePostListStart() }
                .doOnTerminate { onRetrievePostListFinish() }
                .subscribe(
                        { result -> onRetrievePostListSuccess( result ) },
                        { error -> onRetrievePostListError( error ) }
                )
    }

    private fun onRetrievePostListStart()
    {
        loadingVisibility.value = View.VISIBLE
        errorMessage.value = null
    }

    private fun onRetrievePostListFinish()
    {
        loadingVisibility.value = View.GONE
    }

    private fun onRetrievePostListSuccess( postList: List<Post> )
    {
    postListAdapter.updatePostList( postList )
    }

    private fun onRetrievePostListError( error: Throwable )
    {
        println( error )
        errorMessage.value = R.string.post_error
    }
}