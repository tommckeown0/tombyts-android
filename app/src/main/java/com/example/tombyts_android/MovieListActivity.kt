package com.example.tombyts_android

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieListActivity : FragmentActivity() {

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token from intent
        token = intent.getStringExtra("token")

        // Create the leanback browse fragment programmatically
        val fragment = CustomBrowseFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commitNow()
        }
    }

    // Data class for folder navigation
    data class FolderCard(
        val parentFolder: String,
        val folderName: String,
        val files: List<Movie>
    )

    // Custom fragment that extends BrowseSupportFragment
    class CustomBrowseFragment : androidx.leanback.app.BrowseSupportFragment() {

        private var isInSubfolder = false

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            setupUIElements()
            loadMoviesFromAPI()
            setupEventListeners()
        }

        private fun setupUIElements() {
            title = "My Movies"
            headersState = HEADERS_ENABLED
            isHeadersTransitionOnBackEnabled = true
            brandColor = ContextCompat.getColor(requireActivity(), android.R.color.holo_blue_dark)
            searchAffordanceColor = ContextCompat.getColor(requireActivity(), android.R.color.white)
        }

        private fun loadMoviesFromAPI() {
            val token = activity?.intent?.getStringExtra("token")

            if (token == null) {
                Toast.makeText(activity, "No authentication token", Toast.LENGTH_LONG).show()
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val response = Classes.ApiProvider.apiService.getMovies("Bearer $token")
                    if (response.isSuccessful) {
                        val movies = response.body() ?: emptyList()
                        createRowsFromMovies(movies)
                    } else {
                        Log.e("MovieListActivity", "Failed to fetch movies: ${response.code()}")
                        Toast.makeText(activity, "Failed to load movies", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("MovieListActivity", "Error fetching movies: ${e.message}")
                    Toast.makeText(activity, "Error loading movies", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun createRowsFromMovies(movies: List<Movie>) {
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = SimpleCardPresenter()

            isInSubfolder = false // We're at the top level

            // Group by the first folder in the path
            val groupedByFirstFolder = movies.groupBy { movie ->
                movie.path.split("/")[0]
            }

            // Sort folders alphabetically
            val sortedGroups = groupedByFirstFolder.toList().sortedBy { it.first }

            sortedGroups.forEach { (folderName, movieList) ->
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                // Check if this folder has subfolders or just files
                val hasSubfolders = movieList.any { it.path.split("/").size > 1 }

                if (hasSubfolders) {
                    // Has subfolders: Group by next level folder
                    val subfolderGroups = movieList.groupBy { movie ->
                        val pathParts = movie.path.split("/")
                        if (pathParts.size > 1) pathParts[1] else "Files"
                    }.toSortedMap()

                    subfolderGroups.forEach { (subfolderName, files) ->
                        val folderCard = FolderCard(folderName, subfolderName, files)
                        listRowAdapter.add(folderCard)
                    }
                } else {
                    // No subfolders: Show files directly
                    val sortedMovies = movieList.sortedBy { it.title }
                    sortedMovies.forEach { movie ->
                        listRowAdapter.add(movie)
                    }
                }

                val header = HeaderItem(folderName.hashCode().toLong(), folderName)
                rowsAdapter.add(ListRow(header, listRowAdapter))
            }

            adapter = rowsAdapter
        }

        private fun setupEventListeners() {
            setOnSearchClickedListener {
                Toast.makeText(activity, "Search clicked", Toast.LENGTH_LONG).show()
            }

            onItemViewClickedListener = ItemViewClickedListener()
        }

        private inner class ItemViewClickedListener : OnItemViewClickedListener {
            override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder,
                item: Any,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row
            ) {
                when (item) {
                    is Movie -> {
                        Log.d("MovieListActivity", "Movie clicked: ${item.title}")

                        // Navigate to MoviePlayerScreen via MainActivity
                        val token = activity?.intent?.getStringExtra("token")
                        if (token != null) {
                            val intent = android.content.Intent(activity, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                // Add extra to indicate we came from leanback UI
                                putExtra("came_from_leanback", true)
                            }

                            // Create the navigation route that MainActivity expects
                            val encodedTitle = android.net.Uri.encode(item.title)
                            intent.putExtra("navigate_to", "moviePlayer/${encodedTitle}/${token}")

                            startActivity(intent)
                        } else {
                            Toast.makeText(activity, "No token available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is FolderCard -> {
                        Log.d("MovieListActivity", "Folder clicked: ${item.parentFolder}/${item.folderName}")
                        showFilesInFolder(item)
                    }
                }
            }
        }

        private fun showFilesInFolder(folderCard: FolderCard) {
            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = SimpleCardPresenter()
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)

            isInSubfolder = true // We're in a subfolder now

            // Sort files by title
            val sortedFiles = folderCard.files.sortedBy { it.title }

            sortedFiles.forEach { file ->
                listRowAdapter.add(file)
            }

            val header = HeaderItem(0, "${folderCard.parentFolder}/${folderCard.folderName}")
            rowsAdapter.add(ListRow(header, listRowAdapter))

            adapter = rowsAdapter
        }

        // Handle back button press
        override fun onResume() {
            super.onResume()
        }

        // Override the fragment's key event handling
        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // Set up back key handling only for back button
            view.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    if (isInSubfolder) {
                        // Go back to main view
                        loadMoviesFromAPI()
                        true // Consume the event
                    } else {
                        // Let the system handle it (exit the activity)
                        false
                    }
                } else {
                    // Let all other keys pass through normally
                    false
                }
            }
        }

        private fun extractEpisodeNumber(path: String): Int {
            return when {
                path.contains("E\\d+".toRegex()) -> {
                    Regex("E(\\d+)").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                path.contains("/ep") -> {
                    Regex("/ep(\\d+)").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                else -> 0
            }
        }
    }

    // Simple card presenter for displaying movies
    class SimpleCardPresenter : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val cardView = androidx.leanback.widget.ImageCardView(parent.context).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setMainImageDimensions(313, 176)
            }
            return ViewHolder(cardView)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val cardView = viewHolder.view as androidx.leanback.widget.ImageCardView

            when (item) {
                is Movie -> {
                    cardView.titleText = item.title
                    cardView.contentText = "Click to play"
                    cardView.mainImageView.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(cardView.context, android.R.color.darker_gray)
                    )
                }
                is FolderCard -> {
                    cardView.titleText = item.folderName
                    cardView.contentText = "${item.files.size} files"
                    cardView.mainImageView.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(cardView.context, android.R.color.holo_blue_dark)
                    )
                }
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val cardView = viewHolder.view as androidx.leanback.widget.ImageCardView
            cardView.badgeImage = null
            cardView.mainImage = null
        }
    }
}