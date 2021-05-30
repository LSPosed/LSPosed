package org.lsposed.manager.ui.fragment;

import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import org.lsposed.manager.R;

public class BaseFragment extends Fragment {
    public void navigateUp() {
        getNavController().navigateUp();
    }

    public NavController getNavController() {
        View view = getView();
        if (view == null) {
            return null;
        }
        View tabletFragmentContainer = view.findViewById(R.id.tablet_nav_container);
        if (tabletFragmentContainer != null) {
            return Navigation.findNavController(tabletFragmentContainer);
        } else {
            return Navigation.findNavController(view);
        }
    }

    public void setupToolbar(Toolbar toolbar, int title) {
        setupToolbar(toolbar, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, int title, int menu) {
        setupToolbar(toolbar, getString(title), menu);
    }

    public void setupToolbar(Toolbar toolbar, String title, int menu) {
        toolbar.setNavigationOnClickListener(v -> navigateUp());
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setTitle(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
            onPrepareOptionsMenu(toolbar.getMenu());
        }
    }

    public NavOptions getNavOptions() {
        return new NavOptions.Builder()
                .setEnterAnim(R.anim.nav_default_enter_anim)
                .setExitAnim(R.anim.nav_default_exit_anim)
                .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                .build();
    }
}
