package com.adamnickle.reptrack.ui.completedExerciseSet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.*
import com.adamnickle.reptrack.AppExecutors
import com.adamnickle.reptrack.R
import com.adamnickle.reptrack.databinding.CompletedExerciseSetFragmentBinding
import com.adamnickle.reptrack.model.workout.Exercise
import com.adamnickle.reptrack.model.workout.ExerciseSet
import com.adamnickle.reptrack.model.workout.WorkoutDao
import com.adamnickle.reptrack.ui.ViewModelFactory
import com.adamnickle.reptrack.utils.autoCleared
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class CompletedExerciseSetFragment: DaggerFragment()
{
    interface OnCompletedExerciseSetFragmentInteractionListener
    {
        fun onExerciseSetUncompleted( exercise: Exercise, exerciseSet: ExerciseSet )
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var appExecutors: AppExecutors

    @Inject
    lateinit var workoutDao: WorkoutDao

    private var binding by autoCleared<CompletedExerciseSetFragmentBinding>()

    private var adapter by autoCleared<CompletedSetRepListAdapter>()

    private lateinit var viewModel: CompletedExerciseSetFragmentViewModel

    private var listener: OnCompletedExerciseSetFragmentInteractionListener? = null

    companion object
    {
        private const val EXERCISE_SET_ID_TAG = "exercise_set_id"

        fun newInstance( exerciseSet: ExerciseSet ): CompletedExerciseSetFragment
        {
            val exerciseSetId = exerciseSet.id ?: throw IllegalArgumentException( "Cannot create Completed Exercise Set fragment from unsaved Exercise Set." )

            return CompletedExerciseSetFragment().apply {
                arguments = Bundle().apply {
                    putLong( EXERCISE_SET_ID_TAG, exerciseSetId )
                }
            }
        }
    }

    override fun onCreate( savedInstanceState: Bundle? )
    {
        super.onCreate( savedInstanceState )

        setHasOptionsMenu( true )

        val exerciseSetId = arguments?.getLong( EXERCISE_SET_ID_TAG ) ?: throw IllegalArgumentException( "Missing Exercise Set ID for Completed Exercise Set Fragment" )

        viewModel = ViewModelProviders.of( this, viewModelFactory ).get( CompletedExerciseSetFragmentViewModel::class.java )

        appExecutors.diskIO().execute {
            val exerciseSet = workoutDao.getExerciseSet( exerciseSetId ) ?: throw IllegalArgumentException( "Could not find Exercise Set: $exerciseSetId" )

            appExecutors.mainThread().execute {
                viewModel.exerciseSet = exerciseSet
            }
        }
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View
    {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.completed_exercise_set_fragment,
                container,
                false
        )

        adapter = CompletedSetRepListAdapter( appExecutors ) { setRep ->
            println( "Set Rep Clicked: $setRep" )
        }

        binding.repsList.adapter = adapter

        viewModel.exerciseSetLive.observe( this, Observer { exerciseSet ->
            adapter.submitList( ( 0 until ( exerciseSet?.repCount ?: 0 ) ).toList() )
        } )

        return binding.root
    }

    override fun onCreateOptionsMenu( menu: Menu, inflater: MenuInflater )
    {
        inflater.inflate( R.menu.completed_exercise_set_fragment, menu )
    }

    override fun onOptionsItemSelected( item: MenuItem ): Boolean
    {
        return when( item.itemId )
        {
            R.id.unmark_exercise_set_as_completed -> {
                viewModel.exerciseSet?.let { exerciseSet ->
                    appExecutors.diskIO().execute {
                        workoutDao.unmarkExerciseSetCompleted( exerciseSet )

                        val exercise = workoutDao.getExercise( exerciseSet.exerciseId ) ?: throw IllegalArgumentException( "Could not find Exercise: ${exerciseSet.exerciseId}" )
                        listener?.onExerciseSetUncompleted( exercise, exerciseSet )
                    }
                }
                return true
            }
            else -> super.onOptionsItemSelected( item )
        }
    }

    override fun onAttach( context: Context)
    {
        super.onAttach( context )

        if( context is CompletedExerciseSetFragment.OnCompletedExerciseSetFragmentInteractionListener)
        {
            listener = context
        }
        else
        {
            throw ClassCastException( "$context must implement ${CompletedExerciseSetFragment.OnCompletedExerciseSetFragmentInteractionListener::class}" )
        }
    }

    override fun onDetach()
    {
        super.onDetach()

        listener = null
    }
}