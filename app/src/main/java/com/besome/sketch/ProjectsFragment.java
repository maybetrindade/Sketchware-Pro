package com.besome.sketch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.app.*;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.*;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.export.ExportProjectActivity;
import com.besome.sketch.lib.ui.CircleImageView;
import com.besome.sketch.projects.MyProjectButton;
import com.besome.sketch.projects.MyProjectButtonLayout;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sketchware.remod.R;

import mod.RockAriful.AndroXStudio.*;
import mod.RockAriful.AndroXStudio.GitHubRepoCloner;
import mod.SketchwareUtil;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import a.a.a.DA;
import a.a.a.DB;
import a.a.a.MA;
import a.a.a.gB;
import a.a.a.lC;
import a.a.a.mB;
import a.a.a.wB;
import a.a.a.wq;
import a.a.a.yB;
import mod.hey.studios.project.ProjectSettingsDialog;
import mod.hey.studios.project.ProjectTracker;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.agus.jcoderz.lib.FileUtil;

public class ProjectsFragment extends DA implements View.OnClickListener {

    private static final int REQUEST_CODE_DESIGN_ACTIVITY = 204;
    private static final int REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY = 206;
    private static final int REQUEST_CODE_RESTORE_PROJECT = 700;

    private SwipeRefreshLayout swipeRefresh;
    private ArrayList<HashMap<String, Object>> projectsList = new ArrayList<>();
    private RecyclerView myProjects;
    private CardView cvCreateNew;
    private CardView cvRestoreProjects;
    private CardView cvGitCloneProjects;
    private Boolean isCollapsed;
    private AnimatorSet collapseAnimatorSet;
    private AnimatorSet expandAnimatorSet;
    private ProjectsAdapter projectsAdapter;
    private FloatingActionButton floatingActionButton;
    private DB preference;
    private AlertDialog prog;

    private void toProjectSettingOrRequestPermission(int position) {
        if (super.c()) {
            Intent intent = new Intent(getContext(), MyProjectSettingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("sc_id", yB.c(projectsList.get(position), "sc_id"));
            intent.putExtra("is_update", true);
            intent.putExtra("advanced_open", false);
            intent.putExtra("index", position);
            startActivityForResult(intent, REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY);
        } else if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    private void initialize(ViewGroup parent) {
        preference = new DB(getContext(), "project");
        swipeRefresh = parent.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);

            if (c()) {
                refreshProjectsList();
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).s();
            }
        });
        floatingActionButton = getActivity().findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(this);

        myProjects = parent.findViewById(R.id.myprojects);
        myProjects.setHasFixedSize(true);
        myProjects.setLayoutManager(new LinearLayoutManager(getContext()));
        projectsAdapter = new ProjectsAdapter(myProjects);
        myProjects.setAdapter(projectsAdapter);
        myProjects.setItemAnimator(new DefaultItemAnimator());

        cvCreateNew = parent.findViewById(R.id.cv_create_new);
        cvCreateNew.setOnClickListener(this);

        isCollapsed = false;

        cvRestoreProjects = parent.findViewById(R.id.cv_restore_projects);
        cvRestoreProjects.setOnClickListener(this);
        ImageView ivRestoreProjects = parent.findViewById(R.id.iv_restore_projects);
        TextView tvRestoreProjects = parent.findViewById(R.id.tv_restore_projects);

    	cvGitCloneProjects = parent.findViewById(R.id.cv_gitClone_projects);
        cvGitCloneProjects.setOnClickListener(this);
        ImageView ivGitCloneProjects = parent.findViewById(R.id.iv_gitClone_projects);
        TextView tvGitCloneProjects = parent.findViewById(R.id.tv_gitClone_projects);


        collapseAnimatorSet = new AnimatorSet();
        expandAnimatorSet = new AnimatorSet();
        ValueAnimator collapseValueAnimator = ValueAnimator.ofFloat(wB.a(getContext(), 96.0F), wB.a(getContext(), 48.0F));
        collapseValueAnimator.addUpdateListener(valueAnimator -> {
            float value = (Float) valueAnimator.getAnimatedValue();
            cvRestoreProjects.getLayoutParams().height = (int) value;
            cvRestoreProjects.requestLayout();
	    cvGitCloneProjects.getLayoutParams().height = (int) value;
            cvGitCloneProjects.requestLayout();
        });
        ValueAnimator expandValueAnimator = ValueAnimator.ofFloat(wB.a(getContext(), 48.0F), wB.a(getContext(), 96.0F));
        expandValueAnimator.addUpdateListener(valueAnimator -> {
            float value = (Float) valueAnimator.getAnimatedValue();
            cvRestoreProjects.getLayoutParams().height = (int) value;
            cvRestoreProjects.requestLayout();
	    cvGitCloneProjects.getLayoutParams().height = (int) value;
            cvGitCloneProjects.requestLayout();
        });
        collapseAnimatorSet.playTogether(collapseValueAnimator,
                ObjectAnimator.ofFloat(tvRestoreProjects, View.TRANSLATION_Y, 0.0F, -100.0F),
                ObjectAnimator.ofFloat(tvRestoreProjects, View.ALPHA, 1.0F, 0.0F),
                ObjectAnimator.ofFloat(ivRestoreProjects, View.SCALE_X, 1.0F, 0.5F),
                ObjectAnimator.ofFloat(ivRestoreProjects, View.SCALE_Y, 1.0F, 0.5F),

		ObjectAnimator.ofFloat(tvGitCloneProjects, View.TRANSLATION_Y, 0.0F, -100.0F),
                ObjectAnimator.ofFloat(tvGitCloneProjects, View.ALPHA, 1.0F, 0.0F),
                ObjectAnimator.ofFloat(ivGitCloneProjects, View.SCALE_X, 1.0F, 0.5F),
                ObjectAnimator.ofFloat(ivGitCloneProjects, View.SCALE_Y, 1.0F, 0.5F));

        expandAnimatorSet.playTogether(expandValueAnimator,
                ObjectAnimator.ofFloat(tvRestoreProjects, View.TRANSLATION_Y, -100.0F, 0.0F),
                ObjectAnimator.ofFloat(tvRestoreProjects, View.ALPHA, 0.0F, 1.0F),
                ObjectAnimator.ofFloat(ivRestoreProjects, View.SCALE_X, 0.5F, 1.0F),
                ObjectAnimator.ofFloat(ivRestoreProjects, View.SCALE_Y, 0.5F, 1.0F),

		ObjectAnimator.ofFloat(tvGitCloneProjects, View.TRANSLATION_Y, -100.0F, 0.0F),
                ObjectAnimator.ofFloat(tvGitCloneProjects, View.ALPHA, 0.0F, 1.0F),
                ObjectAnimator.ofFloat(ivGitCloneProjects, View.SCALE_X, 0.5F, 1.0F),
                ObjectAnimator.ofFloat(ivGitCloneProjects, View.SCALE_Y, 0.5F, 1.0F));

        collapseAnimatorSet.setDuration(300L);
        expandAnimatorSet.setDuration(300L);
        refreshProjectsList();
    }

    public void a(boolean isEmpty) {
        // Don't load project list without having permissions
        if (!c()) {
            showCreateNewProjectLayout();
            return;
        }

        projectsList = lC.a();
        if (projectsList.size() > 0) {
            //noinspection Java8ListSort
            Collections.sort(projectsList, new ProjectComparator(preference.d("sortBy")));
        }

        myProjects.getAdapter().notifyDataSetChanged();
        if (isEmpty) showCreateNewProjectLayout();
    }

    @Override
    public void b(int requestCode) {
        if (requestCode == REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY) {
            toProjectSettingsActivity();
        } else if (requestCode == REQUEST_CODE_RESTORE_PROJECT) {
            restoreProject();
        }
    }

    private void toDesignActivity(String sc_id) {
        Intent intent = new Intent(getContext(), DesignActivity.class);
        ProjectTracker.setScId(sc_id);
        intent.putExtra("sc_id", sc_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent, REQUEST_CODE_DESIGN_ACTIVITY);
    }

    @Override
    public void c(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void d() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    @Override
    public void e() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    public int getProjectsCount() {
        return projectsList.size();
    }

    private void toExportProjectActivity(int position) {
        Intent intent = new Intent(getContext(), ExportProjectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", yB.c(projectsList.get(position), "sc_id"));
        startActivity(intent);
    }

    public void refreshProjectsList() {
        a(true);
    }

    public void showCreateNewProjectLayout() {
        if (projectsList.size() > 0) {
            cvCreateNew.setVisibility(View.GONE);
            floatingActionButton.show();
        } else {
            cvCreateNew.setVisibility(View.VISIBLE);
            floatingActionButton.hide();
        }
    }

    private void toProjectSettingsActivity() {
        Intent intent = new Intent(getActivity(), MyProjectSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(intent, REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY);
    }

    private void restoreProject() {
        (new BackupRestoreManager(getActivity(), this)).restore();
    }

    private void showProjectSettingDialog(int position) {
        (new ProjectSettingsDialog(getActivity(), yB.c(projectsList.get(position), "sc_id"))).show();
    }

    private void backupProject(int position) {
        String sc_id = yB.c(projectsList.get(position), "sc_id");
        String appName = yB.c(projectsList.get(position), "my_ws_name");
        (new BackupRestoreManager(getActivity())).backup(sc_id, appName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                refreshProjectsList();
                if (data.getBooleanExtra("is_new", false)) {
                    toDesignActivity(data.getStringExtra("sc_id"));
                }
            }
        } else if (requestCode == REQUEST_CODE_RESTORE_PROJECT) {
            if (resultCode == Activity.RESULT_OK) {
                refreshProjectsList();
                restoreProject();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if ((viewId == R.id.cv_create_new || viewId == R.id.fab) && super.a(REQUEST_CODE_PROJECT_SETTINGS_ACTIVITY)) {
            toProjectSettingsActivity();
        } else if (viewId == R.id.cv_restore_projects && super.a(REQUEST_CODE_RESTORE_PROJECT)) {
            restoreProject();
        } else if (viewId == R.id.cv_gitClone_projects && super.a(REQUEST_CODE_RESTORE_PROJECT)) {
            _showCloneRepo();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.myprojects, parent, false);
        initialize(viewGroup);
        return viewGroup;
    }

    @SuppressLint("StaticFieldLeak")
    public class DeleteProjectTask extends MA {
        private final int position;

        public DeleteProjectTask(int position) {
            super(getContext());
            this.position = position;
            ProjectsFragment.this.b();
            addTask(this);
        }

        @Override
        public void a() {
            if (position < projectsList.size()) {
                projectsList.remove(position);
                projectsAdapter.notifyItemRemoved(position);
                projectsAdapter.notifyItemRangeChanged(position, projectsAdapter.getItemCount());
            }

            ProjectsFragment.this.a();
        }

        @Override
        public void a(String idk) {
            ProjectsFragment.this.a();
        }

        @Override
        public void b() {
            if (position < projectsList.size()) {
                lC.a(super.a, yB.c(projectsList.get(position), "sc_id"));
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            return a(voids);
        }
    }

    public class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ViewHolder> {
        private int layoutPosition;

        public ProjectsAdapter(RecyclerView recyclerView) {
            layoutPosition = -1;
            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (dy > 4) {
                            if (isCollapsed) return;
                            collapseAnimatorSet.start();
                            isCollapsed = true;
                        } else {
                            if (dy >= -4 || !isCollapsed) return;
                            expandAnimatorSet.start();
                            isCollapsed = false;
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return projectsList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, Object> projectMap = projectsList.get(position);
            String scId = yB.c(projectMap, "sc_id");
            float rotation;
            int visibility;
            if (yB.a(projectMap, "expand")) {
                visibility = View.VISIBLE;
                rotation = -180.0F;
            } else {
                visibility = View.GONE;
                rotation = 0.0F;
            }
            holder.projectOptionLayout.setVisibility(visibility);
            holder.expand.setRotation(rotation);
            if (yB.a(projectMap, "confirmation")) {
                holder.projectButtonLayout.b();
            } else {
                holder.projectButtonLayout.a();
            }

            holder.imgIcon.setImageResource(R.drawable.default_icon);
            if (yB.c(projectMap, "sc_ver_code").isEmpty()) {
                projectMap.put("sc_ver_code", "1");
                projectMap.put("sc_ver_name", "1.0");
                lC.b(scId, projectMap);
            }

            if (yB.b(projectMap, "sketchware_ver") <= 0) {
                projectMap.put("sketchware_ver", 61);
                lC.b(scId, projectMap);
            }

            if (yB.a(projectMap, "custom_icon")) {
                Uri uri;
                String iconFolder = wq.e() + File.separator + scId;
                if (VERSION.SDK_INT >= 24) {
                    String providerPath = getContext().getPackageName() + ".provider";
                    uri = FileProvider.getUriForFile(getContext(), providerPath, new File(iconFolder, "icon.png"));
                } else {
                    uri = Uri.fromFile(new File(iconFolder, "icon.png"));
                }

                holder.imgIcon.setImageURI(uri);
            }

            holder.appName.setText(yB.c(projectMap, "my_ws_name"));
            holder.projectName.setText(yB.c(projectMap, "my_app_name"));
            holder.packageName.setText(yB.c(projectMap, "my_sc_pkg_name"));
            String version = yB.c(projectMap, "sc_ver_name") + "(" + yB.c(projectMap, "sc_ver_code") + ")";
            holder.projectVersion.setText(version);
            holder.tvPublished.setVisibility(View.VISIBLE);
            holder.tvPublished.setText(yB.c(projectMap, "sc_id"));
            holder.itemView.setTag("custom");
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.myprojects_item, parent, false));
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView tvPublished;
            public final ImageView expand;
            public final MyProjectButtonLayout projectButtonLayout;
            public final LinearLayout projectOptionLayout;
            public final LinearLayout projectOption;
            public final LinearLayout projectOne;
            public final View appIconLayout;
            public final CircleImageView imgIcon;
            public final TextView projectName;
            public final TextView appName;
            public final TextView packageName;
            public final TextView projectVersion;

            public ViewHolder(View itemView) {
                super(itemView);
                projectOne = itemView.findViewById(R.id.project_one);
                projectName = itemView.findViewById(R.id.project_name);
                appIconLayout = itemView.findViewById(R.id.app_icon_layout);
                imgIcon = itemView.findViewById(R.id.img_icon);
                appName = itemView.findViewById(R.id.app_name);
                packageName = itemView.findViewById(R.id.package_name);
                projectVersion = itemView.findViewById(R.id.project_version);
                tvPublished = itemView.findViewById(R.id.tv_published);
                expand = itemView.findViewById(R.id.expand);
                projectOptionLayout = itemView.findViewById(R.id.project_option_layout);
                projectOption = itemView.findViewById(R.id.project_option);
                projectButtonLayout = new MyProjectButtonLayout(getContext());
                projectOption.addView(projectButtonLayout);
                projectButtonLayout.setButtonOnClickListener(v -> {
                    if (!mB.a()) {
                        layoutPosition = getLayoutPosition();
                        if (layoutPosition <= projectsList.size()) {
                            HashMap<String, Object> projectMap = projectsList.get(layoutPosition);
                            if (v instanceof MyProjectButton) {
                                switch (((MyProjectButton) v).b) {
                                    case 0:
                                        toProjectSettingOrRequestPermission(layoutPosition);
                                        break;

                                    case 1:
                                        backupProject(layoutPosition);
                                        break;

                                    case 2:
                                        toExportProjectActivity(layoutPosition);
                                        break;

                                    case 3:
                                        projectMap.put("confirmation", true);
                                        projectButtonLayout.b();
                                        break;

                                    case 4:
                                        showProjectSettingDialog(layoutPosition);
                                        break;
                                }
                            } else {
                                if (v.getId() == R.id.confirm_yes) {
                                    projectMap.put("confirmation", false);
                                    projectMap.put("expand", false);
                                    (new DeleteProjectTask(layoutPosition)).execute();
                                } else if (v.getId() == R.id.confirm_no) {
                                    projectMap.put("confirmation", false);
                                    notifyItemChanged(layoutPosition);
                                }

                            }
                        }
                    }
                });
                projectOne.setOnClickListener(v -> {
                    if (!mB.a()) {
                        layoutPosition = getLayoutPosition();
                        toDesignActivity(yB.c(projectsList.get(layoutPosition), "sc_id"));
                    }
                });
                projectOne.setOnLongClickListener(v -> {
                    layoutPosition = getLayoutPosition();
                    if (yB.a(projectsList.get(layoutPosition), "expand")) {
                        collapse();
                    } else {
                        expand();
                    }

                    return true;
                });
                appIconLayout.setOnClickListener(v -> {
                    mB.a(v);
                    layoutPosition = getLayoutPosition();
                    toProjectSettingOrRequestPermission(layoutPosition);
                });
                expand.setOnClickListener(v -> {
                    if (!mB.a()) {
                        layoutPosition = getLayoutPosition();
                        if (yB.a(projectsList.get(layoutPosition), "expand")) {
                            collapse();
                        } else {
                            expand();
                        }
                    }
                });
            }

            public void collapse() {
                projectsList.get(layoutPosition).put("expand", false);
                gB.a(expand, 0.0F, null);
                gB.a(projectOptionLayout, 300, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        projectOptionLayout.setVisibility(View.GONE);
                    }
                });
            }

            public void expand() {
                projectOptionLayout.setVisibility(View.VISIBLE);
                projectsList.get(layoutPosition).put("expand", true);
                gB.a(expand, -180.0F, null);
                gB.b(projectOptionLayout, 300, null);
            }
        }
    }

	public void _showCloneRepo() {
	    prog = new AlertDialog.Builder(getActivity()).create();
		prog.setCancelable(false);
		prog.setCanceledOnTouchOutside(false);
    	View inflate = getLayoutInflater().inflate(R.layout.add_repo, null);
		prog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
		prog.setView(inflate);
				
        final LinearLayout all_item_layout = (LinearLayout) inflate.findViewById(R.id.all_item_layout);
        final LinearLayout progress_layout = (LinearLayout) inflate.findViewById(R.id.progress_layout);
		        
	    final ProgressBar progressbar1 = (ProgressBar) inflate.findViewById(R.id.progressbar1);	
    	final EditText token = (EditText) inflate.findViewById(R.id.token);
		final EditText username = (EditText) inflate.findViewById(R.id.username);
		final EditText url = (EditText) inflate.findViewById(R.id.url);				
		final TextView no = (TextView) inflate.findViewById(R.id.no);
		final TextView yes = (TextView) inflate.findViewById(R.id.yes);		        		        
		
		username.setFocusableInTouchMode(true);
		token.setFocusableInTouchMode(true);
		url.setFocusableInTouchMode(true);
		progressbar1.setIndeterminate(true);
	    progressbar1.setMax((int)100);
	    progressbar1.setProgress((int)98);

	    yes.setOnClickListener(new View.OnClickListener() {
		  public void onClick(View v) {
								
		   if (url.getText().toString().contains("https://github.com/")) {				
		   
		    no.setEnabled(false);
		    no.setTextColor(0xFFBDBDBD);                    
	 	   all_item_layout.setVisibility(View.GONE);
	        yes.setVisibility(View.GONE);
		    progress_layout.setVisibility(View.VISIBLE);
            
            try{
             String owner = url.getText().toString().replace("https://github.com/", "").replace("/".concat(Uri.parse(url.getText().toString()).getLastPathSegment()), "");
             String repo = Uri.parse(url.getText().toString()).getLastPathSegment().replace(".git","");
             if (new GitHubRepoChecker(token.getText().toString()).execute(owner, repo, "DataSource").get()) {
                 
              GitHubRepoCloner cloner = new GitHubRepoCloner(url.getText().toString(), repo, username.getText().toString(), token.getText().toString());
	  		cloner.cloneRepository(new GitHubRepoCloner.CloneCallback() {
			    @Override
	 		   public void onComplete(boolean success, String SWBFath) {
		         if (success) {
                   prog.dismiss();
                   final ProjectsFragment fragment = ProjectsFragment.this;
           		(new BackupRestoreManager(getActivity(), fragment)).doRestore(SWBFath, true);
		         } else {
                   SketchwareUtil.toastError("Unfortunately An Error Occurred when cloning repository.Try Again!");
                   prog.dismiss();
 		        }
 			   }
                
                @Override
   			 public void onProgress(int progress) {
                  
 			  }
    
		  	});

             }else{SketchwareUtil.toastError("Sorry! This repository is not usable in the Sketchware app.Try again.");prog.dismiss();}   
            } catch (Exception e) {SketchwareUtil.toastError(e.toString());prog.dismiss();}
            
           }
	 	 }
		});
		        
		no.setOnClickListener(new View.OnClickListener() {
	      public void onClick(View v) {
	    	prog.dismiss();
		  }
		});
		prog.show();
	}
}
