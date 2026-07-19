package com.eleeth.tv2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var adapter: ChannelAdapter
    private val channels = mutableListOf<Channel>()
    private val filteredChannels = mutableListOf<Channel>()
    private var currentQuery = ""
    private var currentCategory = "All"
    private var currentChannel: Channel? = null

    // Views
    private lateinit var mainContent: LinearLayout
    private lateinit var fullscreenOverlay: FrameLayout
    private lateinit var videoContainer: FrameLayout
    private lateinit var channelList: RecyclerView
    private lateinit var channelCount: TextView
    private lateinit var searchInput: EditText
    private lateinit var catScroll: LinearLayout
    private lateinit var tipBtn: Button
    private lateinit var emptyView: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var nowPlayingName: TextView
    private lateinit var nowPlayingGroup: TextView
    private lateinit var stopBtn: Button
    private lateinit var fullscreenBtn: Button
    private lateinit var exitFullscreenBtn: Button
    private lateinit var clearSearchBtn: ImageButton
    private lateinit var videoPlaceholder: LinearLayout
    private lateinit var playerView: PlayerView

    // Track where the PlayerView currently lives
    private var playerViewHome: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            bindViews()
            setupPlayer()
            setupSearch()
            setupCategories()
            setupList()
            setupButtons()
            loadM3U()
        } catch (e: Throwable) {
            android.util.Log.e("EleethTV", "onCreate crash", e)
            Toast.makeText(this, "Startup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun bindViews() {
        mainContent = findViewById(R.id.mainContent)
        fullscreenOverlay = findViewById(R.id.fullscreenOverlay)
        videoContainer = findViewById(R.id.videoContainer)
        channelList = findViewById(R.id.channelList)
        channelCount = findViewById(R.id.channelCount)
        searchInput = findViewById(R.id.searchInput)
        catScroll = findViewById(R.id.catScroll)
        tipBtn = findViewById(R.id.tipBtn)
        emptyView = findViewById(R.id.emptyView)
        loadingProgress = findViewById(R.id.loadingProgress)
        nowPlayingName = findViewById(R.id.nowPlayingName)
        nowPlayingGroup = findViewById(R.id.nowPlayingGroup)
        stopBtn = findViewById(R.id.stopBtn)
        fullscreenBtn = findViewById(R.id.fullscreenBtn)
        exitFullscreenBtn = findViewById(R.id.exitFullscreenBtn)
        clearSearchBtn = findViewById(R.id.clearSearchBtn)
        videoPlaceholder = findViewById(R.id.videoPlaceholder)
        playerView = findViewById(R.id.playerView)
    }

    private fun setupPlayer() {
        try {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    runOnUiThread {
                        loadingProgress.visibility =
                            if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        if (state == Player.STATE_READY) {
                            videoPlaceholder.visibility = View.GONE
                        }
                    }
                }
                override fun onPlayerError(e: PlaybackException) {
                    runOnUiThread {
                        loadingProgress.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Stream unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } catch (e: Throwable) {
            android.util.Log.e("EleethTV", "setupPlayer crash", e)
        }
    }

    private fun setupList() {
        adapter = ChannelAdapter { ch -> playChannel(ch) }
        val span = when {
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE -> 4
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> 3
            else -> 2
        }
        channelList.layoutManager = GridLayoutManager(this, span)
        channelList.adapter = adapter
        channelList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(out: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                val d = dp(4)
                out.set(d, d, d, d)
            }
        })
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                currentQuery = s?.toString()?.trim() ?: ""
                clearSearchBtn.visibility = if (currentQuery.isEmpty()) View.GONE else View.VISIBLE
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        searchInput.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                applyFilter()
                true
            } else false
        }
        clearSearchBtn.setOnClickListener {
            searchInput.setText("")
            currentQuery = ""
            clearSearchBtn.visibility = View.GONE
            applyFilter()
            searchInput.clearFocus()
            hideKeyboard()
        }
    }

    private fun setupCategories() {
        val cats = listOf("All", "Movies", "News", "Sports", "Music", "Kids", "Entertainment")
        cats.forEach { addCategoryChip(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun addCategoryChip(label: String) {
        val tv = TextView(this).apply {
            text = label
            setTextColor(0xFFE8E4DF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = androidx.core.content.ContextCompat.getDrawable(this@MainActivity, R.drawable.chip_bg_unselected)
            isClickable = true
            isFocusable = true
            setOnClickListener { selectCategory(label, this) }
        }
        catScroll.addView(tv)
        if (label == "All") selectCategory("All", tv)
    }

    private fun selectCategory(label: String, view: TextView) {
        currentCategory = label
        for (i in 0 until catScroll.childCount) {
            val child = catScroll.getChildAt(i) as TextView
            child.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.chip_bg_unselected)
            child.setTextColor(0xFFE8E4DF.toInt())
        }
        view.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.chip_bg_selected)
        view.setTextColor(0xFF0D0D12.toInt())
        applyFilter()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilter() {
        try {
            val q = currentQuery.trim()
            val cat = currentCategory
            filteredChannels.clear()
            for (ch in channels) {
                val catMatch = cat == "All" ||
                        ch.group.equals(cat, true) ||
                        ch.group.split(";").any { it.trim().equals(cat, true) }
                if (!catMatch) continue
                if (q.isNotEmpty()) {
                    val inName = ch.name.contains(q, ignoreCase = true)
                    val inGroup = ch.group.contains(q, ignoreCase = true)
                    if (!inName && !inGroup) continue
                }
                filteredChannels.add(ch)
            }
            adapter.replace(filteredChannels)
            emptyView.visibility = if (filteredChannels.isEmpty() && channels.isNotEmpty()) View.VISIBLE else View.GONE
            channelCount.text = getString(R.string.channel_count, filteredChannels.size)
        } catch (e: Throwable) {
            android.util.Log.e("EleethTV", "filter crash", e)
        }
    }

    private fun setupButtons() {
        // Set icon drawables on the icon-only buttons
        fullscreenBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        stopBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        exitFullscreenBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

        // Use the icon drawables as background-ish (using setCompoundDrawablesWithIntrinsicBounds is for text buttons)
        // For pure ImageButton-style, we'd swap to ImageButton, but keeping Button for the styled bg
        // Actually we need to set the drawable on the right side of the button (compound drawable)
        fullscreenBtn.setCompoundDrawablesWithIntrinsicBounds(
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_fullscreen),
            null, null, null
        )
        stopBtn.setCompoundDrawablesWithIntrinsicBounds(
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_stop),
            null, null, null
        )
        exitFullscreenBtn.setCompoundDrawablesWithIntrinsicBounds(
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_exit),
            null, null, null
        )

        tipBtn.setOnClickListener { openKofi() }
        stopBtn.setOnClickListener { stopPlayback() }
        fullscreenBtn.setOnClickListener { enterFullscreen() }
        exitFullscreenBtn.setOnClickListener { exitFullscreen() }
    }

    private fun enterFullscreen() {
        if (currentChannel == null) {
            Toast.makeText(this, "Pick a channel first", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            // Move the playerView into the fullscreen overlay (parent reattachment)
            val parent = playerView.parent as? ViewGroup
            parent?.removeView(playerView)
            fullscreenOverlay.addView(playerView, 0, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            playerViewHome = parent
            fullscreenOverlay.visibility = View.VISIBLE
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (e: Exception) {
            android.util.Log.e("EleethTV", "fullscreen error", e)
        }
    }

    private fun exitFullscreen() {
        try {
            // Move playerView back to its original container
            val parent = playerView.parent as? ViewGroup
            parent?.removeView(playerView)
            val home = playerViewHome ?: videoContainer
            home.addView(playerView, 0, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            ))
            fullscreenOverlay.visibility = View.GONE
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Exception) {
            android.util.Log.e("EleethTV", "exit fullscreen error", e)
        }
    }

    private fun openKofi() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/eleeth")))
        } catch (e: Exception) {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        try {
            player?.stop()
            currentChannel = null
            videoPlaceholder.visibility = View.VISIBLE
            nowPlayingName.text = getString(R.string.nothing_playing)
            nowPlayingGroup.text = getString(R.string.pick_channel)
            loadingProgress.visibility = View.GONE
            if (fullscreenOverlay.visibility == View.VISIBLE) {
                exitFullscreen()
            }
        } catch (_: Exception) {}
    }

    private fun loadM3U() {
        loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val out = mutableListOf<Channel>()
            try {
                val paths = listOf(
                    "/mnt/ftpstorage/iptv.m3u",
                    "/mnt/usb/iptv.m3u",
                    "/storage/ftpstorage/iptv.m3u",
                    "/storage/usb0/iptv.m3u",
                    "/storage/usb1/iptv.m3u"
                )
                var content: String? = null
                for (p in paths) {
                    val f = File(p)
                    if (f.exists() && f.length() > 0) {
                        try {
                            content = f.readText()
                            if (!content.isNullOrEmpty()) break
                        } catch (_: Exception) {}
                    }
                }
                if (content == null) {
                    try {
                        content = assets.open("iptv.m3u").bufferedReader().readText()
                    } catch (_: Exception) {}
                }
                if (!content.isNullOrEmpty()) parseM3U(content, out)
            } catch (e: Throwable) {
                android.util.Log.e("EleethTV", "loadM3U crash", e)
            }
            withContext(Dispatchers.Main) {
                channels.clear()
                channels.addAll(out)
                loadingProgress.visibility = View.GONE
                applyFilter()
            }
        }
    }

    private fun parseM3U(content: String, out: MutableList<Channel>) {
        try {
            val lines = content.split("\\r?\\n".toRegex())
            var i = 0
            val inf = Regex("""#EXTINF:-?\d+\s+(.*?),(.*)""")
            val tvgId = Regex("""tvg-id="([^"]*)"""")
            val tvgLogo = Regex("""tvg-logo="([^"]*)"""")
            val groupTitle = Regex("""group-title="([^"]*)"""")
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("#EXTINF:")) {
                    val m = inf.find(line)
                    if (m != null) {
                        val attrs = m.groupValues[1]
                        val name = m.groupValues[2].trim()
                        val id = tvgId.find(attrs)?.groupValues?.get(1) ?: ""
                        val logo = tvgLogo.find(attrs)?.groupValues?.get(1) ?: ""
                        val group = groupTitle.find(attrs)?.groupValues?.get(1) ?: ""
                        i++
                        if (i < lines.size) {
                            val url = lines[i].trim()
                            if (url.startsWith("http")) out.add(Channel(id, name, logo, group, url))
                        }
                    }
                }
                i++
            }
        } catch (e: Throwable) {
            android.util.Log.e("EleethTV", "parseM3U crash", e)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun playChannel(ch: Channel) {
        try {
            currentChannel = ch
            nowPlayingName.text = ch.name
            nowPlayingGroup.text = ch.group.split(";").firstOrNull()?.trim() ?: ""
            loadingProgress.visibility = View.VISIBLE
            videoPlaceholder.visibility = View.GONE
            player?.setMediaItem(MediaItem.fromUri(ch.url))
            player?.prepare()
            player?.playWhenReady = true
            hideKeyboard()
        } catch (e: Throwable) {
            loadingProgress.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fullscreenOverlay.visibility == View.VISIBLE) {
                exitFullscreen()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        try { player?.pause() } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try { player?.play() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { player?.release() } catch (_: Exception) {}
        player = null
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
        } catch (_: Exception) {}
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}