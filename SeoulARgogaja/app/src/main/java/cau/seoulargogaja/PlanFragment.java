package cau.seoulargogaja;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.design.widget.FloatingActionButton;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;


import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cau.seoulargogaja.adapter.PlanAdapter;
import cau.seoulargogaja.data.IdDAO;
import cau.seoulargogaja.data.MainState;
import cau.seoulargogaja.data.PlanDAO;
import cau.seoulargogaja.data.PlanDTO;
import cau.seoulargogaja.data.PlanListDAO;
import cau.seoulargogaja.data.PlanListDTO;
import cau.seoulargogaja.data.WalletDAO;
import cau.seoulargogaja.data.WalletDTO;

import static android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;

public class PlanFragment extends Fragment {


    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton fab, fab1, fab2, fab3;
    private TextView editTitle;
    private TextView startDate,endDate;
    private ImageView startImage,endImage,menu_list;
    // datePicker 사용
    private SimpleDateFormat dateFormatter;
    private DatePickerDialog dialog,dialog2;


    private Date sDate,eDate;
    private int Date_diff_all;
    private int Date_diff_month;
    private int Date_diff_date;

    PlanDAO dao;
    PlanListDAO listdao;
    InputMethodManager imm;
    ArrayList<PlanDTO> list;
    ViewGroup rootView;
    MainState mainState;
    private int row_count;
    ArrayList<String> dates;
    Activity activity;
    IdDAO iddao;
    PlanAdapter adapter;

    @Override
    public void onResume(){
        super.onResume();
        Log.d("PlanFragment","hi");
        set_plan_list(rootView);
        activity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = (ViewGroup)inflater.inflate(R.layout.fragment_plan, container, false);
        dao = new PlanDAO(this.getActivity());
        listdao = new PlanListDAO(this.getActivity());
        //iddao = new IdDAO(this.getActivity());


        set_plan_list(rootView);

        imm = (InputMethodManager)this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        return rootView;
    }

    private void set_plan_list(ViewGroup rootView){
        //임시로 MainState만듬 0으로 planlistid set 해놓음 Splash추가후 거기서 DB에서 읽어오는것으로 해야할듯
        // PLANLIST DB에 첫번째를 mainstate로 설정
        listdao = new PlanListDAO(this.getActivity());
        iddao = new IdDAO(this.getActivity());
        PlanListDTO mainPlan = listdao.select_one(iddao.select());
        mainState = new MainState(mainPlan);
        editTitle = (TextView) rootView.findViewById(R.id.plan_title);
        editTitle.setText(mainState.getMainDto().getName());

        try {
            String from = mainState.getStartDate();
            sDate = new SimpleDateFormat("yyyy-MM-dd").parse(from);
            from = mainState.getEndDate();
            eDate = new SimpleDateFormat("yyyy-MM-dd").parse(from);
            dates = new ArrayList<String>();
            all_Date(sDate,eDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        startDate = (TextView) rootView.findViewById(R.id.txt_calender);
        endDate = (TextView) rootView.findViewById(R.id.txt_calender2);

        startDate.setText(mainState.getStartDate());
        endDate.setText(mainState.getEndDate());

        list = dao.select_planlistid(mainState.getplanlistId(),dates);

        PlanDTO plus_plan = new PlanDTO();
        list.add(plus_plan);
        row_count = list.size()-1;//0부터 시작하니 마지막위치는 -1

        final CustomListView listView = (CustomListView)rootView.findViewById(R.id.listView1);
        adapter = new PlanAdapter(getActivity(), list, new PlanAdapter.Listener() {
            @Override
            public void onGrab(int position, RelativeLayout row) {
                listView.onGrab(position, row);
            }
        });

        listView.setAdapter(adapter);
        listView.setListener(new CustomListView.Listener() {
            @Override
            public void swapElements(int indexOne, int indexTwo) {

                if(indexOne != row_count && indexTwo != row_count && indexOne != 0 && indexTwo != 0){
                    PlanDTO temp1 = list.get(indexOne);
                    PlanDTO temp2 = list.get(indexTwo);

                    dao.Change_two_order(temp1,temp2);

                    int temp_order1 = temp1.getOrder();
                    int temp_order2 = temp2.getOrder();
                    temp1.setOrder(temp_order2);
                    temp2.setOrder(temp_order1);

                    list.set(indexOne, temp2);
                    list.set(indexTwo, temp1);

                }
            }
        });

        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);

        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab1 = (FloatingActionButton) rootView.findViewById(R.id.fab1);
        fab2 = (FloatingActionButton) rootView.findViewById(R.id.fab2);
        fab3 = (FloatingActionButton) rootView.findViewById(R.id.fab3);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                anim();
            }
        });

        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*TEST 새로운 planlist만들기*/
                PlanListDTO newlist = new PlanListDTO();
                newlist.setStartDate(getTime());
                newlist.setEnddate(getTime());
                newlist.setBudget(0);
                listdao.insert(newlist);
                int newlistid = listdao.last_id();
                newlist.setId(newlistid);
                iddao.update(newlistid);
                MainState mainState = new MainState(newlist);
                Toast.makeText(getActivity(), "새로운 일정이 만들어졌습니다.", Toast.LENGTH_SHORT).show();
                onResume();
                anim();
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                regist_planlist();
                regist_plan();
                regist_wallet();
                Intent intent = new Intent(getActivity(), QRmakeActivity.class);
                startActivity(intent);
                anim();
            }
        });

        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), QRreadActivity.class);
                startActivity(intent);
                anim();
            }
        });



        menu_list = (ImageView) rootView.findViewById(R.id.menu_list);
        // add 버튼 누르면 plan 추가 화면으로 돌아감
        menu_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SelectPlanlist.class);
                startActivity(intent);
            }
        });


        editTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());

                ad.setTitle("제목을 입력해 주세요");       // 제목 설정

                // EditText 삽입하기
                final EditText et = new EditText(getActivity());
                et.setText(editTitle.getText());
                ad.setView(et);

                // 확인 버튼 설정
                ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = et.getText().toString();
                        editTitle.setText(value);
                        mainState.getMainDto().setName(value);
                        listdao = new PlanListDAO(activity);
                        listdao.update_name(mainState.getMainDto());
                        dialog.dismiss();     //닫기
                        // Event
                    }
                });
                // 취소 버튼 설정
                ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();     //닫기
                        // Event
                    }
                });

                // 창 띄우기
                ad.show();
            }
        });

        // 달력모양 입력 시 입력되는 형태
        startImage = (ImageView) rootView.findViewById(R.id.btn_calender);
        //startDate.setInputType(InputType.TYPE_NULL);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd",Locale.KOREA);
        // 달력모양 눌렀을 때 datePicker 띄우기
        Calendar newCalendar = Calendar.getInstance();
        dialog = new DatePickerDialog(getActivity(), THEME_DEVICE_DEFAULT_LIGHT, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                sDate = newDate.getTime();
                if(check_Date_diff(sDate,eDate)){
                    startDate.setText(dateFormatter.format(sDate));
                    //MainState랑 PlanlistID DB변경할 필요있음
                    mainState.setStartDate(dateFormatter.format(sDate));
                    //mainState.setEnddate(dateFormatter.format(eDate));
                    PlanListDAO dao = new PlanListDAO(activity);
                    dao.update(mainState.getMainDto());
                    onResume();
                }
            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        startImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        // 달력모양 입력 시 입력되는 형태
        endImage = (ImageView) rootView.findViewById(R.id.btn_calender2);
        //endDate.setInputType(InputType.TYPE_NULL);
        // 달력모양 눌렀을 때 datePicker 띄우기
        Calendar newCalendar2 = Calendar.getInstance();
        dialog2 = new DatePickerDialog(getActivity(), THEME_DEVICE_DEFAULT_LIGHT, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate2 = Calendar.getInstance();
                newDate2.set(year, monthOfYear, dayOfMonth);
                eDate = newDate2.getTime();
                if(check_Date_diff(sDate,eDate)){
                    endDate.setText(dateFormatter.format(eDate));
                    //MainState랑 PlanlistID DB변경할 필요있음
                    //mainState.setStartDate(dateFormatter.format(sDate));
                    mainState.setEnddate(dateFormatter.format(eDate));
                    PlanListDAO dao = new PlanListDAO(activity);
                    dao.update(mainState.getMainDto());
                    onResume();
                }
            }
        }, newCalendar2.get(Calendar.YEAR), newCalendar2.get(Calendar.MONTH), newCalendar2.get(Calendar.DAY_OF_MONTH));
        endImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog2.show();
            }
        });

    }


    public void regist_planlist(){
        mainState = new MainState();
        PlanListDTO planListDTO = mainState.getMainDto();
        try {
            Phprequest request = new Phprequest(Phprequest.BASE_URL + "regist_planlist.php");
            request.regist_planlist(planListDTO.getId(),planListDTO.getName(),planListDTO.getStartDate(),planListDTO.getEndDate(),planListDTO.getBudget(),planListDTO.getCode());
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public void regist_plan(){
        mainState = new MainState();
        PlanDAO dao = new PlanDAO(this.getActivity());
        ArrayList<PlanDTO> plan_list = dao.select_planlistid(mainState.getplanlistId(),dates);
        try {
            for(PlanDTO item:plan_list) {
                Phprequest request = new Phprequest(Phprequest.BASE_URL + "regist_plan.php");
                request.regist_plan(item.getId(),item.getContent(),item.getdate(),item.getspotID(),item.getStamp(),item.getLatitude(),item.getLongitude(),item.getmemo(),item.getOrder(),item.getdatatype(),item.getplanlistid());
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public void regist_wallet(){
        mainState = new MainState();
        WalletDAO dao = new WalletDAO(this.getActivity());
        ArrayList<WalletDTO> wallet_list = dao.select_planlistid(mainState.getplanlistId(),dates);
        try {
            for(WalletDTO item:wallet_list) {
                Phprequest request = new Phprequest(Phprequest.BASE_URL + "regist_wallet.php");
                request.regist_wallet(item.getId(),item.getdate(),item.getplanlistid(),item.getdetail(),item.getexpend(),item.getmemo(),item.getdatatype(),item.getmain_image(),item.getsub_image(),item.getcolor_type(),item.getOrder());
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public boolean check_Date_diff(Date sDate,Date eDate){
        if(sDate != null && eDate != null){
            if(eDate.compareTo(sDate) < 0){
                Toast.makeText(getActivity(), "날짜가 안 맞습니다. 날짜를 확인해 주세요", Toast.LENGTH_SHORT).show();
                Toast.makeText(getActivity(), Integer.toString(eDate.compareTo(sDate)), Toast.LENGTH_SHORT).show();
                return false;
            }
            Date_diff_all = Date_diff(sDate,eDate);
            Date_diff(sDate,Date_diff_all);
            all_Date(sDate,eDate);
            return true;
        }
        return true;
    }

    public int Date_diff(Date sDate,Date eDate){
        try{
            //SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
            long calDate = sDate.getTime() - eDate.getTime();

            // Date.getTime() 은 해당날짜를 기준으로1970년 00:00:00 부터 몇 초가 흘렀는지를 반환해준다.
            // 이제 24*60*60*1000(각 시간값에 따른 차이점) 을 나눠주면 일수가 나온다.
            long calDateDays = calDate / ( 24*60*60*1000);

            calDateDays = Math.abs(calDateDays);
            Toast.makeText(getActivity(), "두 날짜의 날짜 차이:"+(int)calDateDays, Toast.LENGTH_SHORT).show();
            return (int)calDateDays;
        }
        catch(Exception e)
        {
            return -1;
            // 예외 처리
        }
    }

    public void Date_diff(Date sDate,int Date_diff_all){
        try{
            String diff_M;
            SimpleDateFormat dateFormat_mm = new SimpleDateFormat("MM", java.util.Locale.getDefault());
            diff_M = dateFormat_mm.format(sDate);
            Date_diff_month = Integer.parseInt(diff_M);
            Toast.makeText(getActivity(), "첫달의 월 : "+Date_diff_month, Toast.LENGTH_SHORT).show();

            String diff_d;
            SimpleDateFormat dateFormat_dd = new SimpleDateFormat("dd", java.util.Locale.getDefault());
            diff_d = dateFormat_dd.format(sDate);
            Date_diff_date = Integer.parseInt(diff_d);
            Toast.makeText(getActivity(), "첫달의 일 : "+Date_diff_date, Toast.LENGTH_SHORT).show();

        }
        catch(Exception e)
        {
            // 예외 처리
        }
    }

    public void all_Date(Date sDate,Date eDate){
        dao = new PlanDAO(this.getActivity());
        listdao = new PlanListDAO(this.getActivity());

        final String DATE_PATTERN = "yyyy-MM-dd";
        Date currentDate = sDate;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        while (currentDate.compareTo(eDate) <= 0) {
            dates.add(sdf.format(currentDate));
            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);
            c.add(Calendar.DAY_OF_MONTH, 1);
            currentDate = c.getTime();
        }
        mainState.setdates(dates);
        dao.insert_date(dates,mainState.getplanlistId());
        dao.test_sql_order(mainState.getplanlistId());
        /*
        for (String date : dates) {
            System.out.println(date);
        }*/

    }

    public void anim() {

        if (isFabOpen) {
            fab.setImageResource(R.drawable.ic_add_black_24dp);
            fab1.startAnimation(fab_close);
            fab2.startAnimation(fab_close);
            fab3.startAnimation(fab_close);
            fab1.setClickable(false);
            fab2.setClickable(false);
            fab3.setClickable(false);
            isFabOpen = false;
        } else {
            fab.setImageResource(R.drawable.ic_close_black_24dp);
            fab1.startAnimation(fab_open);
            fab2.startAnimation(fab_open);
            fab3.startAnimation(fab_open);
            fab1.setClickable(true);
            fab2.setClickable(true);
            fab3.setClickable(true);
            isFabOpen = true;
        }
    }

    public ArrayList<PlanListDTO> getPlanList() {
        PlanListDAO dao = new PlanListDAO(this.getActivity());
        return dao.selectAll();
    }

    private String getTime(){
        long mNow;
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    /**
     * NestedFragment에서 startactivityForresult실행시 fragment에 들어오지 않는 문제
     *
     * @param activityResultEvent
     */

    @Subscribe
    public void onActivityResult(ActivityResultEvent activityResultEvent) {
        adapter.onActivityResult(activityResultEvent.getRequestCode(), activityResultEvent.getResultCode(), activityResultEvent.getData());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onDestroyView() {
        BusProvider.getInstance().unregister(this);
        super.onDestroyView();

    }







}


