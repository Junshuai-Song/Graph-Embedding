ppm = [400 400 400 400];        % points per mixture
centers = [7.5 7.5; 7.5 12.5; 12.5 7.5; 12.5 12.5];
stdev = 1;
[data2,labels]=makegaussmixnd(centers,stdev,ppm);
plotcol (data2, ppm, 'rgbk');

printt ('original_7p5_12p5_400');

X = data2(:,1).*0.6;
Y = data2(:,2).*0.6;
data3 = [X.*cos(X) Y X.*sin(X)];
plotcol (data3,ppm,'rgbk');

[m,n]=size(data3);

disp(m)
disp(n)


a = data3
fid=fopen('a.txt','w');%写入文件路径
[m,n]=size(a);
for i=1:1:m
    for j=1:1:n
        if j==n
            fprintf(fid,'%3.4f ',a(i,j));
            if i<=400
                fprintf(fid,'0\n');
            else
                if i<=800
                    fprintf(fid,'1\n');
                else
                    if i<=1200
                        fprintf(fid,'2\n');
                    else
                        fprintf(fid,'3\n');
                    end
                end
            end
        else
            fprintf(fid,'%3.4f ',a(i,j));
        end
    end
end
fclose(fid);
