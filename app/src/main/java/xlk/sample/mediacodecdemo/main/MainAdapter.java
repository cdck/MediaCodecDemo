package xlk.sample.mediacodecdemo.main;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import androidx.annotation.Nullable;
import xlk.sample.mediacodecdemo.R;

/**
 * @author Created by xlk on 2020/9/26.
 * @desc
 */
public class MainAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public MainAdapter(@Nullable List<String> data) {
        super(R.layout.item_main, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.item_btn, item);
    }
}
