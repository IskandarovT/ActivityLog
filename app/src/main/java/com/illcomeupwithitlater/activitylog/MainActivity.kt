package com.melkii_mel.activitylog

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.melkii_mel.activitylog.data.Userdata
import com.melkii_mel.activitylog.fragments.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    lateinit var toolbarTitle: TextView
    lateinit var homeFragment: HomeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Userdata.initUserdata(this)

        homeFragment = HomeFragment()
        setFragment(homeFragment)

        toolbar = findViewById(R.id.toolbar)
        toolbarTitle = findViewById(R.id.toolbar_title)

        toolbar.title = ""
        setSupportActionBar(toolbar)
        setBackEnabled(false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFragment == homeFragment) {
                    finish()
                    return
                }
                toolbarTitle.text = getString(R.string.activity_logger)
                setBackEnabled(false)
                setFragment(homeFragment)
            }
        })
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        initFilePickerLauncher()
        initFileSaverLauncher()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Overflow menu action handlers
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                saveFileWithUserSelection(
                    Userdata.FULL_FILE_NAME,
                    "application/json",
                    Userdata.current.toJsonString()
                )
                true
            }

            R.id.action_share -> {
                val applicationId = BuildConfig.APPLICATION_ID
                val file = Userdata.current.toJsonFile(this)
                @Suppress("SpellCheckingInspection") val uri =
                    FileProvider.getUriForFile(this, "${applicationId}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent,
                    getString(R.string.share_userdata_as_json)))
                file.deleteOnExit()
                true
            }

            R.id.action_import_userdata -> {
                AlertDialog.Builder(this).apply {
                    setTitle(getString(R.string.import_userdata) + "?")
                    setMessage(getString(R.string.file_import_warning))
                    setPositiveButton(getString(R.string._import)) { dialog, _ ->
                        dialog.dismiss()
                        pickFile("application/json") { _, _, content ->
                            Userdata.deserializeFromJsonString(content, onSuccess = {
                                Userdata.current.serialize(this@MainActivity)
                                setFragment(homeFragment)
                                homeFragment.activityViewRecyclerAdapter.changeDataSet(Userdata.current.activityInfos)
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    getString(R.string.userdata_imported_successfully),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }, onError = {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    getString(R.string.failed_to_import_userdata),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            })
                        }
                    }
                    setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
                true
            }

            R.id.action_import_activity -> {
                pickFile("*/*") { _, type, content ->
                    if (type != "csv") {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            getString(R.string.invalid_type_csv_file_expected),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        return@pickFile
                    }
                    NewActivityPopupHandler.show(this, null, true) {
                        Userdata.current.addActivityInfoFromCsv(this, content, it, onError = { errorString ->
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                errorString ?: getString(R.string.failed_to_import_activity),
                                Snackbar.LENGTH_INDEFINITE
                            ).show()
                        }, onSuccess = {
                            Userdata.current.serialize(this)
                            homeFragment.activityViewRecyclerAdapter.notifyItemInserted(Userdata.current.activityInfos.size - 1)
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                getString(R.string.activity_imported_successfully),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        })
                    }
                }
                return true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    fun setBackEnabled(on: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(on)
    }

    private lateinit var currentFragment: Fragment
    fun setFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .commit()
    }
}