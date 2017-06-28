package sh.alexious.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private TextView mInputView, mOutputView, mWordsView;

    private ArrayList<String> messages = null;
    private String words = null;
    private Pattern wordsPattern = null;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputView = (TextView) findViewById(R.id.input);
        mOutputView = (TextView) findViewById(R.id.output);
        mWordsView = (TextView) findViewById(R.id.words);

        final ImageButton filterButton = (ImageButton) findViewById(R.id.go);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //filterMessages();
            }
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();

        messages = new ArrayList<>();

        Query myMessagesQuery = mDatabase.child("messages");
        myMessagesQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear();

                for (DataSnapshot msgSnapshot: dataSnapshot.getChildren()) {
                    String message = msgSnapshot.getValue(String.class);

                    if(message != null)
                        messages.add(message);
                }

                String out = messages != null ? TextUtils.join("\n", messages) : null;
                mInputView.setText(out);
                filterMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        Query myWordsQuery = mDatabase.child("words");
        myWordsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder sb = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                sb.append("\\b(");

                for (DataSnapshot wordSnapshot: dataSnapshot.getChildren()) {
                    if(sb.length() > 4) {
                        sb.append("|");
                        sb2.append(", ");
                    }

                    sb.append(wordSnapshot.getValue(String.class));
                    sb2.append(wordSnapshot.getValue(String.class));
                }

                sb.append(")\\b");
                words = sb2.toString();
                wordsPattern = Pattern.compile(sb.toString(),
                        Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

                mWordsView.setText(words);
                filterMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String filterMessage(String message) {
        Matcher m = wordsPattern.matcher(message);
        StringBuffer sb = new StringBuffer(message.length());

        while (m.find()) {
            m.appendReplacement(sb, m.group(1).replaceAll("(?su).", "*"));
        }

        m.appendTail(sb);
        return sb.toString();
    }

    private synchronized void filterMessages() {
        StringBuilder sb = new StringBuilder();

        if (messages != null) {
            for (String msg : messages) {
                if (sb.length() > 0)
                    sb.append("\n");

                sb.append(filterMessage(msg));
            }
        }

        mOutputView.setText(sb);
    }
}
